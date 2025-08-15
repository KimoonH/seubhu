import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// --- 테스트 옵션 설정 (100명 동시 접속) ---
export const options = {
    stages: [
        { duration: '1m', target: 1000 }
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<500'],
    },
};

// --- 테스트 데이터 설정 (파라미터로 사용할 값) ---
// topNo와 catNo가 모두 존재하는 경우
const detailedParams = new SharedArray('detailed parameters', function () {
    return [
        { topNo: 10, catNo: 11, rows: 6, sort: 'date' },
        { topNo: 10, catNo: 12, rows: 6, sort: 'date' },
        { topNo: 10, catNo: 13, rows: 6, sort: 'date' },
        { topNo: 10, catNo: 14, rows: 6, sort: 'date' },
        { topNo: 20, catNo: 21, rows: 6, sort: 'date' },
        { topNo: 20, catNo: 22, rows: 6, sort: 'date' },
        { topNo: 20, catNo: 23, rows: 6, sort: 'date' },
        { topNo: 20, catNo: 24, rows: 6, sort: 'date' },
        { topNo: 30, catNo: 31, rows: 6, sort: 'date' },
        { topNo: 30, catNo: 32, rows: 6, sort: 'date' },
    ];
});

// catNo 없이 topNo만 존재하는 경우
const generalParams = new SharedArray('general parameters', function () {
    return [
        { topNo: 10, rows: 6, sort: 'date' },
        { topNo: 20, rows: 6, sort: 'date' },
        { topNo: 30, rows: 6, sort: 'date' },
    ];
});

// --- 메인 테스트 로직 ---
export default function () {
    // 50% 확률로 상세 요청, 50% 확률로 전체 요청
    let randomParam;
    if (Math.random() < 0.5) {
        randomParam = detailedParams[Math.floor(Math.random() * detailedParams.length)];
    } else {
        randomParam = generalParams[Math.floor(Math.random() * generalParams.length)];
    }

    const page = Math.floor(Math.random() * 5) + 1;

    const url = `http://localhost:8080/product/list`;

    // 쿼리 파라미터 객체 동적 생성
    const query = {
        topNo: randomParam.topNo,
        page: page,
        rows: randomParam.rows,
        sort: randomParam.sort,
    };
    // catNo가 있는 경우에만 쿼리 파라미터에 추가
    if (randomParam.catNo) {
        query.catNo = randomParam.catNo;
    }
    // opt와 value는 이 요청에서 제외

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        query: query,
    };

    const res = http.get(url, params);

    check(res, {
        'is status 200': (r) => r.status === 200,
        'body is not empty': (r) => r.body.length > 0,
    });

    sleep(1);
}