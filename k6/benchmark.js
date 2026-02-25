import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    stages: [
        { duration: '30s', target: 20 },  
        { duration: '1m', target: 50 },   
        { duration: '10s', target: 0 },   
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],  
    },
};

const BASE_URL = 'http://localhost:8080/api/auth';

export default function () {
    const uniqueUsername = `testuser_${randomString(8)}_${__VU}_${__ITER}`;
    const password = 'password123';

    const registerPayload = JSON.stringify({
        username: uniqueUsername,
        password: password,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const resRegister = http.post(`${BASE_URL}/register`, registerPayload, params);

    check(resRegister, {
        'register status is 201': (r) => r.status === 201,
        'register response has token or data': (r) => r.json() !== undefined
    });
    
    sleep(1);
}
