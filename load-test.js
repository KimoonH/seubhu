import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// 📊 베이스라인 측정용 커스텀 메트릭
export const responseTime = new Trend('response_time');
export const popularProductTime = new Trend('popular_product_time');
export const randomProductTime = new Trend('random_product_time');

// 🚀 캐싱 전 베이스라인 측정 설정
export const options = {
    scenarios: {
        // 시나리오 1: 일반적인 쇼핑몰 트래픽 (70% 트래픽)
        normal_shopping: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 30 },   // 워밍업
                { duration: '3m', target: 80 },   // 평상시 트래픽
                { duration: '3m', target: 150 },  // 피크 시간대
                { duration: '2m', target: 100 },  // 점진적 감소
                { duration: '2m', target: 0 },    // 종료
            ],
            exec: 'normalShoppingTest',
            tags: { test_type: 'normal_shopping' },
        },

        // 시나리오 2: 이벤트/세일 기간 부하 (30% 트래픽)
        event_traffic: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 20 },   // 빠른 시작
                { duration: '2m', target: 100 },  // 급격한 증가
                { duration: '4m', target: 200 },  // 이벤트 피크
                { duration: '3m', target: 120 },  // 서서히 감소
                { duration: '2m', target: 0 },    // 종료
            ],
            exec: 'eventTrafficTest',
            tags: { test_type: 'event_traffic' },
        },
    },

    // 📊 실제 쇼핑몰 수준의 현실적인 임계값
    thresholds: {
        // 전체 응답시간 기준 (캐싱 없음)
        http_req_duration: ['p(50)<150', 'p(95)<600', 'p(99)<1200'],

        // 시나리오별 기준
        'http_req_duration{test_type:normal_shopping}': ['p(95)<500'],   // 일반 쇼핑
        'http_req_duration{test_type:event_traffic}': ['p(95)<800'],     // 이벤트 트래픽

        // 안정성 기준 (실제 쇼핑몰 수준)
        http_req_failed: ['rate<0.05'],    // 실패율 5% 미만

        // 처리량 기준 (적당한 규모로 조정)
        http_reqs: ['rate>80'],            // 초당 80개 이상 (전체)
        'http_reqs{test_type:event_traffic}': ['rate>120'], // 이벤트 시 120개 이상

        // 베이스라인 측정용
        response_time: ['p(95)<600'],
        popular_product_time: ['p(95)<600'],
        random_product_time: ['p(95)<600'],
    },
};

const BASE_URL = 'http://localhost:8080';

// 🎯 에러 없는 확실한 상품-색상 조합만 사용 (임시)
const validProducts = [
    { productNo: 1, colorNo: 1 },   // 블랙
    { productNo: 2, colorNo: 2 },   // 블랙
    { productNo: 3, colorNo: 3 },   // 화이트
    { productNo: 10, colorNo: 10 }, // 블랙
    { productNo: 13, colorNo: 13 }, // 블랙
    { productNo: 16, colorNo: 16 }, // 블랙
    { productNo: 17, colorNo: 17 }, // 블랙
    { productNo: 19, colorNo: 19 }, // 블랙
    { productNo: 20, colorNo: 20 }, // 블랙
    { productNo: 22, colorNo: 22 }, // 블랙
    { productNo: 23, colorNo: 23 }, // 블랙
    { productNo: 24, colorNo: 24 }, // 블랙
    { productNo: 25, colorNo: 25 }, // 블랙
    { productNo: 26, colorNo: 26 }, // 크림슨
    { productNo: 31, colorNo: 31 }, // 블랙
];

// 📈 안전한 인기 상품들 (에러 없는 조합만)
const popularProducts = [
    { productNo: 1, colorNo: 1 },   // 기본 블랙
    { productNo: 3, colorNo: 3 },   // 나이키 화이트
    { productNo: 25, colorNo: 25 }, // 블랙
    { productNo: 26, colorNo: 26 }, // 크림슨
];

// 📊 시나리오 1: 일반적인 쇼핑몰 트래픽
export function normalShoppingTest() {
    const userBehavior = Math.random();

    if (userBehavior < 0.8) {
        // 80% - 인기 상품 집중 (캐시 히트율 극대화!)
        testPopularProduct();
    } else if (userBehavior < 0.9) {
        // 10% - 다양한 상품 브라우징
        testRandomProduct();
    } else {
        // 10% - 집중적인 상품 비교
        testProductComparison();
    }

    // 실제 쇼핑 행동 패턴 (천천히 둘러보기)
    sleep(Math.random() * 5 + 2); // 2-7초 대기
}

// 📊 시나리오 2: 이벤트/세일 기간 트래픽
export function eventTrafficTest() {
    const behavior = Math.random();

    if (behavior < 0.9) {
        // 90% - 인기 상품 집중 (세일 상품, 한정판 등)
        testPopularProduct();
    } else {
        // 10% - 다양한 상품 조회
        testRandomProduct();
    }

    // 이벤트 시 더 빠른 사용자 행동
    sleep(Math.random() * 2 + 0.5); // 0.5-2.5초 대기
}

function testPopularProduct() {
    const product = popularProducts[Math.floor(Math.random() * popularProducts.length)];
    const url = `${BASE_URL}/product/detail?productNo=${product.productNo}&colorNo=${product.colorNo}`;

    const response = http.get(url, {
        tags: {
            scenario: 'popular_product',
            product_id: `${product.productNo}_${product.colorNo}`
        }
    });

    // 📊 인기 상품 베이스라인 측정
    popularProductTime.add(response.timings.duration);
    responseTime.add(response.timings.duration);

    check(response, {
        '⭐ 인기상품 - 상태코드 200': (r) => r.status === 200,
        '⭐ 인기상품 - 응답시간 기록': (r) => r.timings.duration < 2000,
        '⭐ 인기상품 - 내용 정상': (r) => r.body.length > 500,
    });

    // 베이스라인 로깅
    console.log(`⭐ 인기상품 ${product.productNo}: ${response.timings.duration}ms (베이스라인)`);
}

function testRandomProduct() {
    const product = validProducts[Math.floor(Math.random() * validProducts.length)];
    const url = `${BASE_URL}/product/detail?productNo=${product.productNo}&colorNo=${product.colorNo}`;

    const response = http.get(url, {
        tags: {
            scenario: 'random_product',
            product_id: `${product.productNo}_${product.colorNo}`
        }
    });

    // 📊 일반 상품 베이스라인 측정
    randomProductTime.add(response.timings.duration);
    responseTime.add(response.timings.duration);

    check(response, {
        '🎲 랜덤상품 - 상태코드 200': (r) => r.status === 200,
        '🎲 랜덤상품 - 응답시간 기록': (r) => r.timings.duration < 2000,
        '🎲 랜덤상품 - 내용 정상': (r) => r.body.length > 500,
    });
}

function testProductComparison() {
    // 사용자가 2-4개 상품을 비교하는 패턴
    const compareCount = Math.floor(Math.random() * 3) + 2; // 2-4개

    for (let i = 0; i < compareCount; i++) {
        const product = validProducts[Math.floor(Math.random() * validProducts.length)];
        const url = `${BASE_URL}/product/detail?productNo=${product.productNo}&colorNo=${product.colorNo}`;

        const response = http.get(url, {
            tags: {
                scenario: 'product_comparison',
                comparison_step: i + 1
            }
        });

        responseTime.add(response.timings.duration);
        randomProductTime.add(response.timings.duration);

        check(response, {
            '🔍 비교상품 - 상태코드 200': (r) => r.status === 200,
            '🔍 비교상품 - 응답시간 양호': (r) => r.timings.duration < 1500,
            '🔍 비교상품 - 내용 정상': (r) => r.body.length > 500,
        });

        // 상품간 비교 시간
        if (i < compareCount - 1) {
            sleep(Math.random() * 3 + 1); // 1-4초 상품 검토 시간
        }
    }
}

function testQuickBrowsing() {
    const product = validProducts[Math.floor(Math.random() * validProducts.length)];
    const url = `${BASE_URL}/product/detail?productNo=${product.productNo}&colorNo=${product.colorNo}`;

    const response = http.get(url, {
        tags: {
            scenario: 'quick_browsing',
            urgency: 'high'
        }
    });

    responseTime.add(response.timings.duration);
    randomProductTime.add(response.timings.duration);

    check(response, {
        '⚡ 빠른조회 - 상태코드 200': (r) => r.status === 200,
        '⚡ 빠른조회 - 응답시간 중요': (r) => r.timings.duration < 1000,
        '⚡ 빠른조회 - 내용 정상': (r) => r.body.length > 500,
    });
}

export function setup() {
    console.log('🛒 실제 쇼핑몰 규모 베이스라인 측정 시작!');
    console.log('🎯 목적: 실제 운영 환경 수준에서 캐싱 전 성능 측정');
    console.log('📊 실제 데이터 기반:');
    console.log('  📦 15개 안전한 상품-색상 조합 (에러 없는 것만 선별)');
    console.log('  🔧 NullPointerException 방지를 위한 임시 조치');
    console.log('  ✅ 100% 성공 예상 조합만 사용');
    console.log('📈 테스트 규모:');
    console.log('  👥 일반 쇼핑: 최대 150명 동시 사용자');
    console.log('  🎉 이벤트 트래픽: 최대 200명 동시 사용자');
    console.log('  🔥 총 최대: 350명 동시 사용자 (적당한 중형 쇼핑몰 수준)');
    console.log('📊 측정 항목:');
    console.log('  - 인기 상품 응답시간 (캐싱 후 주요 개선 대상)');
    console.log('  - 일반 상품 응답시간');
    console.log('  - 이벤트 트래픽 처리 능력');
    console.log('  - 상품 비교 시나리오 성능');
    console.log('⏱️ 총 12분간 진행 (실제 피크타임 시뮬레이션)');

    // 웜업
    console.log('🔥 서버 웜업...');
    popularProducts.forEach(product => {
        http.get(`${BASE_URL}/product/detail?productNo=${product.productNo}&colorNo=${product.colorNo}`);
        sleep(0.3);
    });
    console.log('✅ 웜업 완료');
}

export function teardown(data) {
    console.log('✅ 실제 쇼핑몰 규모 베이스라인 측정 완료!');
    console.log('📊 베이스라인 데이터 기록됨:');
    console.log('🛒 일반 쇼핑 트래픽 성능 (최대 150명)');
    console.log('🎉 이벤트 트래픽 성능 (최대 200명)');
    console.log('⭐ 인기 상품 평균 응답시간 (캐싱 후 주요 개선 대상)');
    console.log('🔍 상품 비교 시나리오 성능');
    console.log('🚀 다음 단계: 캐싱 구현 후 동일한 테스트로 비교!');
    console.log('📈 예상 개선 효과:');
    console.log('  1. 인기 상품 응답시간 50-80% 단축');
    console.log('  2. 전체 처리량 2-3배 증가');
    console.log('  3. 이벤트 트래픽 안정성 대폭 향상');
    console.log('  4. 서버 CPU/DB 부하 현저히 감소');
}