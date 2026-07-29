import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    normal_users: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 8),
      duration: __ENV.DURATION || '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  },
};

const baseUrl = (__ENV.BASE_URL || 'http://frontend').replace(/\/$/, '');

export default function () {
  const health = http.get(`${baseUrl}/api/v1/health`);
  check(health, { 'health is up': (response) => response.status === 200 });

  const loginPage = http.get(`${baseUrl}/login`);
  check(loginPage, { 'frontend loads': (response) => response.status === 200 });

  sleep(1);
}
