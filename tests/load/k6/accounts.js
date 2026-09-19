import http from 'k6/http';
import { check } from 'k6';
import { accountsUrl, headers, commonThresholds } from './lib/config.js';

export const options = { thresholds: commonThresholds, scenarios: { steady: { executor: 'constant-arrival-rate', rate: 40, timeUnit: '1s', duration: '2m', preAllocatedVUs: 20, maxVUs: 100 } } };

export default function () {
  const response = http.get(`${accountsUrl}/accounts/v1/me`, { headers, tags: { endpoint: 'accounts.me' } });
  check(response, { 'profile is successful': (value) => value.status === 200 });
}
