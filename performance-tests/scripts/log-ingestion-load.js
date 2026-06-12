import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const LEVELS = ['INFO', 'WARN', 'ERROR', 'DEBUG'];
const SERVICES = ['api-gateway', 'auth-service', 'log-processor', 'anomaly-detector'];

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '60s', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

function pick(values) {
  return values[Math.floor(Math.random() * values.length)];
}

function randomLog() {
  return {
    timestamp: new Date().toISOString(),
    level: pick(LEVELS),
    service: pick(SERVICES),
    message: `synthetic load event ${Math.random().toString(36).slice(2)}`,
  };
}

export default function () {
  const payload = JSON.stringify(randomLog());
  const params = { headers: { 'Content-Type': 'application/json' } };

  const res = http.post(`${BASE_URL}/api/v1/logs`, payload, params);

  check(res, {
    'status is 202': (r) => r.status === 202,
  });

  sleep(0.1);
}
