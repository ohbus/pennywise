import http from 'k6/http';
import { check } from 'k6';
import { notificationsUrl, headers, commonThresholds } from './lib/config.js';

export const options = { thresholds: commonThresholds, scenarios: { inbox: { executor: 'constant-arrival-rate', rate: 25, timeUnit: '1s', duration: '2m', preAllocatedVUs: 15, maxVUs: 80 } } };

export default function () {
  const response = http.get(`${notificationsUrl}/notifications/v1/inbox`, { headers, tags: { endpoint: 'notifications.inbox' } });
  check(response, { 'inbox read is successful': (value) => value.status === 200 });
}
