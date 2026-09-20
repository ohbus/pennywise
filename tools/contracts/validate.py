"""Dependency-light contract syntax validation used before tool-specific lint."""
import json
from pathlib import Path

root = Path(__file__).parents[2]
files = sorted((root / "contracts").rglob("*.json"))
for path in files:
    json.loads(path.read_text(encoding="utf-8"))
    print(f"valid JSON: {path.relative_to(root)}")

schema = root / "contracts/graphql/schema.graphqls"
text = schema.read_text(encoding="utf-8")
required = ["type Query", "type Mutation", "type Subscription", "scalar MoneyMinor"]
missing = [item for item in required if item not in text]
if missing:
    raise SystemExit(f"GraphQL contract missing required declarations: {missing}")
print(f"valid GraphQL declaration set: {schema.relative_to(root)}")

registry = json.loads((root / "docs/tasks/registry.yaml").read_text(encoding="utf-8"))
ids = {task["id"] for task in registry["tasks"]}
missing_deps = [(task["id"], dep) for task in registry["tasks"] for dep in task["depends_on"] if dep not in ids]
missing_specs = [task["specification"] for task in registry["tasks"] if not (root / task["specification"]).exists()]
if missing_deps or missing_specs:
    raise SystemExit({"missing_dependencies": missing_deps, "missing_specs": missing_specs})
print(f"valid task registry: {len(registry['tasks'])} tasks")
