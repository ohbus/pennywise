import http from 'k6/http';
import { check } from 'k6';
import { baseUrl, accountsUrl, expenseCoreUrl, notificationsUrl, headers } from './lib/config.js';

const duration = __ENV.DURATION || '10m';
const participantId = __ENV.PARTICIPANT_ID || '00000000-0000-7000-8000-000000000001';

export const options = {
  scenarios: {
    accounts: { executor: 'constant-arrival-rate', rate: 150, timeUnit: '1s', duration, preAllocatedVUs: 100, maxVUs: 500, exec: 'accounts' },
    expenseReads: { executor: 'constant-arrival-rate', rate: 400, timeUnit: '1s', duration, preAllocatedVUs: 200, maxVUs: 800, exec: 'expenseReads' },
    expenseWrites: { executor: 'constant-arrival-rate', rate: 50, timeUnit: '1s', duration, preAllocatedVUs: 50, maxVUs: 250, exec: 'expenseWrites' },
    notifications: { executor: 'constant-arrival-rate', rate: 100, timeUnit: '1s', duration, preAllocatedVUs: 75, maxVUs: 350, exec: 'notifications' },
    graphql: { executor: 'constant-arrival-rate', rate: 300, timeUnit: '1s', duration, preAllocatedVUs: 150, maxVUs: 700, exec: 'graphql' },
  },
  thresholds: {
    http_req_failed: ['rate<0.001'],
    'http_req_duration{surface:accounts}': ['p(95)<250', 'p(99)<500'],
    'http_req_duration{surface:expense-read}': ['p(95)<400', 'p(99)<800'],
    'http_req_duration{surface:expense-write}': ['p(95)<600', 'p(99)<1200'],
    'http_req_duration{surface:notifications}': ['p(95)<300', 'p(99)<600'],
    'http_req_duration{surface:graphql}': ['p(95)<500', 'p(99)<1000'],
  },
};

export function setup() {
  const response = http.post(`${expenseCoreUrl}/expense-core/v1/groups`, JSON.stringify({ name: `k6-baseline-${Date.now()}`, kind: 'TRIP', currency: 'EUR' }), {
    headers: { ...headers, 'Content-Type': 'application/json' }, tags: { surface: 'fixture-group' },
  });
  check(response, { 'baseline fixture group created': (value) => value.status === 201 });
  if (response.status !== 201) throw new Error(`baseline fixture setup failed: ${response.status} ${response.body}`);
  return { groupId: response.json('groupId') };
}

export function teardown(fixture) {
  if (!fixture?.groupId) return;
  const response = http.post(`${expenseCoreUrl}/expense-core/v1/groups/${fixture.groupId}/archive`, null, { headers, tags: { surface: 'fixture-cleanup' } });
  check(response, { 'baseline fixture group archived': (value) => value.status === 200 });
}

export function accounts() { get(`${accountsUrl}/accounts/v1/me`, 'accounts'); }
export function expenseReads() { get(`${expenseCoreUrl}/expense-core/v1/groups`, 'expense-read'); }
export function notifications() { get(`${notificationsUrl}/notifications/v1/inbox`, 'notifications'); }
export function graphql() { post(`${baseUrl}/graphql`, '{ me { accountId displayName defaultCurrency } }', 'graphql'); }
export function expenseWrites(fixture) {
  const expenseId = uuid();
  const payload = { expenseId, description: 'k6 1M baseline expense', category: 'other', amount: { currency: 'EUR', minor: '100' },
    payers: [{ participantId, amount: { currency: 'EUR', minor: '100' } }],
    allocation: { mode: 'EQUAL', items: [{ participantId, value: '1' }] } };
  const response = http.post(`${expenseCoreUrl}/expense-core/v1/groups/${fixture.groupId}/expenses`, JSON.stringify(payload), {
    headers: { ...headers, 'Content-Type': 'application/json', 'Idempotency-Key': expenseId }, tags: { surface: 'expense-write' },
  });
  check(response, { 'expense write is successful': (value) => value.status === 201 });
}

function get(url, surface) {
  const response = http.get(url, { headers, tags: { surface } });
  check(response, { [`${surface} response is successful`]: (value) => value.status === 200 });
}
function post(url, query, surface) {
  const response = http.post(url, JSON.stringify({ query }), { headers: { ...headers, 'Content-Type': 'application/json' }, tags: { surface } });
  check(response, { 'graphql response is successful': (value) => value.status === 200 && value.body.includes('data') });
}

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
    const random = Math.random() * 16 | 0;
    const value = character === 'x' ? random : (random & 0x3 | 0x8);
    return value.toString(16);
  });
}
