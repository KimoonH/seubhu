import http from 'k6/http';
import { check, sleep } from 'k6';

// 🔧 간단한 테스트 설정 (문제 파악용)
export const options = {
    vus: 1,        // 1명만
    duration: '30s', // 30초만
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    console.log('=== 테스트 시작 ===');

    // 🔍 1. 홈페이지 먼저 테스트
    testHomePage();

    sleep(1);

    // 🔍 2. 상품 상세 페이지 테스트 (확실한 번호로)
    testProductDetailSafe();

    sleep(1);

    // 🔍 3. 상품 목록 테스트
    testProductListSafe();
}

function testHomePage() {
    console.log('홈페이지 테스트 중...');
    const response = http.get(`${BASE_URL}/`);

    const success = check(response, {
        '홈페이지 - 상태코드 200': (r) => r.status === 200,
    });

    console.log(`홈페이지 결과: 상태=${response.status}, 응답시간=${response.timings.duration}ms`);

    if (!success) {
        console.error('홈페이지 접속 실패!');
    }
}

function testProductDetailSafe() {
    // 🔥 확실히 존재하는 상품으로 테스트
    const productNo = 72; // 문서에서 확인된 번호
    const colorNo = 71;   // 문서에서 확인된 번호

    console.log(`상품 상세 테스트: productNo=${productNo}, colorNo=${colorNo}`);

    const url = `${BASE_URL}/product/detail?productNo=${productNo}&colorNo=${colorNo}`;
    console.log(`요청 URL: ${url}`);

    const response = http.get(url);

    console.log(`상품 상세 결과: 상태=${response.status}, 응답시간=${response.timings.duration}ms`);
    console.log(`응답 크기: ${response.body.length}bytes`);

    const success = check(response, {
        '상품 상세 - 상태코드 200': (r) => r.status === 200,
        '상품 상세 - 응답 내용 있음': (r) => r.body.length > 100,
    });

    if (!success) {
        console.error(`상품 상세 실패! 상태: ${response.status}`);
        console.error(`응답 내용 일부: ${response.body.substring(0, 200)}`);
    } else {
        console.log('✅ 상품 상세 성공!');
    }
}

function testProductListSafe() {
    console.log('상품 목록 테스트 중...');

    // 🔥 가능한 URL들 시도
    const possibleUrls = [
        `${BASE_URL}/product/list`,
        `${BASE_URL}/product/list?page=1`,
        `${BASE_URL}/products`,
        `${BASE_URL}/products?page=1`,
    ];

    for (const url of possibleUrls) {
        console.log(`시도 중: ${url}`);
        const response = http.get(url);

        console.log(`결과: 상태=${response.status}, 크기=${response.body.length}`);

        if (response.status === 200) {
            console.log(`✅ 성공한 URL: ${url}`);
            break;
        } else {
            console.log(`❌ 실패한 URL: ${url}`);
        }

        sleep(0.5);
    }
}

export function teardown() {
    console.log('=== 테스트 완료 ===');
    console.log('다음 단계:');
    console.log('1. 성공한 URL 확인');
    console.log('2. 실제 존재하는 상품/색상 번호 확인');
    console.log('3. 문제 해결 후 본격 부하테스트 진행');
}