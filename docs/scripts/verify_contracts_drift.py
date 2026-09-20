import glob
import json
import os
import re
from typing import Dict, List, Set, Tuple, Any

def get_api_endpoints_map() -> Dict[str, str]:
    with open('libs/ids/src/main/kotlin/com/subhrodip/pennywise/ids/contracts/ApiEndpoints.kt', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # Track current context/object
    context_stack = []
    constants = {}

    for line in lines:
        line_clean = line.strip()
        obj_m = re.match(r'object\s+([A-Za-z0-9_]+)', line_clean)
        if obj_m:
            context_stack.append(obj_m.group(1))
        elif line_clean == '}':
            if context_stack:
                context_stack.pop()
        
        const_m = re.match(r'const\s+val\s+([A-Za-z0-9_]+)\s*:\s*String\s*=\s*(.*)', line_clean)
        if const_m:
            name = const_m.group(1)
            raw_val = const_m.group(2).strip().strip('"')
            scope = ".".join(context_stack)
            full_key = f"{scope}.{name}" if scope else name
            constants[full_key] = raw_val
            constants[name] = raw_val

    # Resolve template substitutions
    for _ in range(5):
        for k, v in list(constants.items()):
            resolved = v
            for ref_k, ref_v in constants.items():
                if f"${ref_k}" in resolved:
                    resolved = resolved.replace(f"${ref_k}", ref_v)
            constants[k] = resolved

    return constants

def evaluate_path_expression(expr: str, class_prefix: str, constants: Dict[str, str], service_base: str) -> str:
    expr = expr.strip()
    # Check if expr references ApiEndpoints
    # e.g., ApiEndpoints.Accounts.V1.LOGIN_START
    # e.g., ApiEndpoints.Accounts.V1.PATH_LOGIN_START
    # e.g., ApiEndpoints.ExpenseCore.V1.INVITE_CLAIM
    val = ""
    for k, v in constants.items():
        if k in expr:
            val = v
            break
    
    if not val:
        # direct string literal?
        m = re.search(r'["\']([^"\']*)["\']', expr)
        if m:
            val = m.group(1)
        else:
            val = expr
    
    # If val starts with service_base (e.g. /accounts/v1/...), strip the service_base to get the OpenAPI relative path
    # Because OpenAPI paths in Pennywise are relative to servers url (/accounts/v1, /expense-core/v1, /notifications/v1)
    if class_prefix and not val.startswith(service_base):
        combined = (class_prefix.rstrip('/') + '/' + val.lstrip('/')).replace('//', '/')
    else:
        combined = val

    if combined.startswith(service_base):
        combined = combined[len(service_base):]
    
    if not combined.startswith('/'):
        combined = '/' + combined
    
    return combined

def audit_rest_contracts_vs_code():
    print("=================================================================")
    print("AUDIT 1: REST CONTRACTS VS SPRING CONTROLLER IMPLEMENTATIONS")
    print("=================================================================")
    
    constants = get_api_endpoints_map()
    
    services = {
        'accounts': {
            'contract': 'contracts/rest/accounts.openapi.json',
            'code_dir': 'app/accounts/src/main/kotlin',
            'server_base': '/accounts/v1'
        },
        'expense-core': {
            'contract': 'contracts/rest/expense-core.openapi.json',
            'code_dir': 'app/expense-core/src/main/kotlin',
            'server_base': '/expense-core/v1'
        },
        'notifications': {
            'contract': 'contracts/rest/notifications.openapi.json',
            'code_dir': 'app/notifications/src/main/kotlin',
            'server_base': '/notifications/v1'
        }
    }

    all_matched = True

    for svc_name, cfg in services.items():
        print(f"\n--- Service: {svc_name} (Base: {cfg['server_base']}) ---")
        with open(cfg['contract'], 'r', encoding='utf-8') as f:
            spec = json.load(f)
        
        contract_ops = {}
        for path, path_item in spec.get('paths', {}).items():
            for method in ['get', 'post', 'put', 'delete', 'patch']:
                if method in path_item:
                    op = path_item[method]
                    op_id = op.get('operationId', f"{method.upper()} {path}")
                    contract_ops[(method.upper(), path)] = {
                        'operationId': op_id,
                        'params': [p.get('name') for p in op.get('parameters', [])],
                        'status': path_item.get('x-implementation-status') or op.get('x-implementation-status')
                    }
        
        # Scan controllers
        kt_files = glob.glob(f"{cfg['code_dir']}/**/*.kt", recursive=True)
        controllers = []
        for kf in kt_files:
            with open(kf, 'r', encoding='utf-8', errors='ignore') as f:
                code = f.read()
                if '@RestController' in code or '@Controller' in code:
                    controllers.append((kf, code))
        
        extracted_endpoints = []
        for kf, code in controllers:
            # Class level RequestMapping
            class_prefix = ""
            m_class = re.search(r'@RequestMapping\(\s*([^)]*)\)', code)
            if m_class:
                class_prefix = evaluate_path_expression(m_class.group(1), "", constants, cfg['server_base'])
                # If class prefix resolved to '/', keep it empty or '/'
                if class_prefix == '/':
                    class_prefix = ""
            
            # Match handler methods
            # Matches @GetMapping(...), @PostMapping(...), etc.
            method_pattern = re.compile(r'@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\s*(?:\(\s*([^)]*)\))?\s*(?:@[A-Za-z0-9_]+(?:\([^)]*\))?\s*)*fun\s+([a-zA-Z0-9_]+)', re.MULTILINE)
            for m in method_pattern.finditer(code):
                ann = m.group(1)
                args = m.group(2) or ""
                fun_name = m.group(3)
                http_method = ann.replace('Mapping', '').upper()
                
                route_path = evaluate_path_expression(args, class_prefix, constants, cfg['server_base'])
                extracted_endpoints.append({
                    'method': http_method,
                    'path': route_path,
                    'fun_name': fun_name,
                    'file': os.path.basename(kf)
                })

        # Match contract against extracted endpoints
        matched_ops = []
        unmatched_ops = []
        for (c_method, c_path), c_info in contract_ops.items():
            found = False
            for ep in extracted_endpoints:
                if ep['method'] == c_method and ep['path'] == c_path:
                    found = True
                    break
            if found:
                matched_ops.append((c_method, c_path, c_info['operationId']))
            else:
                unmatched_ops.append((c_method, c_path, c_info['operationId']))
        
        print(f"Contract operations: {len(contract_ops)}")
        print(f"Controller endpoints extracted: {len(extracted_endpoints)}")
        print(f"Matched: {len(matched_ops)}/{len(contract_ops)}")
        
        if unmatched_ops:
            all_matched = False
            print("  UNMATCHED CONTRACT OPERATIONS:")
            for um in unmatched_ops:
                print(f"    - {um[0]} {um[1]} (operationId: {um[2]})")
            print("  EXTRACTED ENDPOINTS WERE:")
            for ep in extracted_endpoints:
                print(f"    * {ep['method']} {ep['path']} in {ep['file']}::{ep['fun_name']}")

    return all_matched

if __name__ == '__main__':
    audit_rest_contracts_vs_code()
