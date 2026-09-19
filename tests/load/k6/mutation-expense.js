import http from 'k6/http';
import { check } from 'k6';
import { expenseCoreUrl, headers } from './lib/config.js';

const duration = __ENV.DURATION || '10s';
const participantId = __ENV.PARTICIPANT_ID || '00000000-0000-7000-8000-000000000001';

export const options = {
  scenarios: { expenseWrites: { executor: 'constant-arrival-rate', rate: 50, timeUnit: '1s', duration, preAllocatedVUs: 50, maxVUs: 250 } },
  thresholds: { http_req_failed: ['rate<0.001'], http_req_duration: ['p(95)<600', 'p(99)<1200'] },
};

export function setup() {
  const response = http.post(`${expenseCoreUrl}/expense-core/v1/groups`, JSON.stringify({ name: `k6-write-${Date.now()}`, kind: 'TRIP', currency: 'EUR' }), {
    headers: { ...headers, 'Content-Type': 'application/json' }, tags: { surface: 'fixture-group' },
  });
  check(response, { 'fixture group created': (value) => value.status === 201 });
  if (response.status !== 201) throw new Error(`fixture setup failed: ${response.status} ${response.body}`);
  return { groupId: response.json('groupId') };
}

export function teardown(fixture) {
  if (!fixture?.groupId) return;
  const response = http.post(`${expenseCoreUrl}/expense-core/v1/groups/${fixture.groupId}/archive`, null, { headers, tags: { surface: 'fixture-cleanup' } });
  check(response, { 'fixture group archived': (value) => value.status === 200 });
}

export default function (fixture) {
  const expenseId = uuid();
  const payload = { expenseId, description: 'k6 capacity fixture', category: 'other', amount: { currency: 'EUR', minor: '100' },
    payers: [{ participantId, amount: { currency: 'EUR', minor: '100' } }],
    allocation: { mode: 'EQUAL', items: [{ participantId, value: '1' }] } };
  const response = http.post(`${expenseCoreUrl}/expense-core/v1/groups/${fixture.groupId}/expenses`, JSON.stringify(payload), {
    headers: { ...headers, 'Content-Type': 'application/json', 'Idempotency-Key': expenseId }, tags: { surface: 'expense-write' },
  });
  if (response.status !== 201 && __ITER === 0) console.log(`expense fixture response ${response.status}: ${response.body}`);
  check(response, { 'expense created': (value) => value.status === 201 });
}

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
    const random = Math.random() * 16 | 0;
    const value = character === 'x' ? random : (random & 0x3 | 0x8);
    return value.toString(16);
  });
}
