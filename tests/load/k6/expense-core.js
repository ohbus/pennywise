import http from 'k6/http';
import { check } from 'k6';
import { expenseCoreUrl, headers, commonThresholds } from './lib/config.js';

export const options = { thresholds: commonThresholds, scenarios: { reads: { executor: 'constant-arrival-rate', rate: 30, timeUnit: '1s', duration: '2m', preAllocatedVUs: 20, maxVUs: 100 } } };

export default function () {
  const response = http.get(`${expenseCoreUrl}/expense-core/v1/groups`, { headers, tags: { endpoint: 'expense-core.groups' } });
  check(response, { 'groups read is successful': (value) => value.status === 200 });
}
