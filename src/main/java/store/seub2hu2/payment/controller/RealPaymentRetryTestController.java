package store.seub2hu2.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.service.PaymentService;

/**
 * 🧪 실제 결제 재시도 테스트용 컨트롤러
 * 테스트 완료 후 삭제하세요!
 */
@RestController
@RequestMapping("/test-real-payment")
@RequiredArgsConstructor
@Slf4j
public class RealPaymentRetryTestController {

    private final PaymentService paymentService;

    /**
     * 실제 결제 승인 재시도 테스트
     * 사용법: http://localhost:8080/test-real-payment/approve
     */
    @GetMapping("/approve")
    public String testRealPaymentRetry() {
        try {
            log.info("🧪 실제 결제 재시도 테스트 시작");

            // 테스트용 더미 데이터
            String testTid = "test_tid_" + System.currentTimeMillis();
            String testPgToken = "test_pg_token_123";
            int testOrderNo = 99999;

            // 실제 PaymentService의 @Retryable 메서드 호출!
            ApproveResponse result = paymentService.processPaymentApproval(testTid, testPgToken, testOrderNo);

            log.info("🎉 재시도 테스트 성공!");
            return "✅ 재시도 테스트 성공! TID: " + testTid;

        } catch (Exception e) {
            log.error("❌ 재시도 테스트 실패", e);
            return "❌ 재시도 테스트 실패: " + e.getMessage();
        }
    }

    /**
     * 도움말
     */
    @GetMapping("/help")
    public String showHelp() {
        return """
            🧪 실제 결제 재시도 테스트
            
            1. 테스트 실행: GET /test-real-payment/approve
            2. 콘솔 로그 확인하세요!
            
            기대하는 결과:
            - 첫 번째 시도 실패 (KakaoPayService에서 예외 발생)
            - 1초 대기
            - 두 번째 시도 성공
            
            로그에서 재시도 과정을 확인할 수 있습니다! 📝
            """;
    }

    /**
     * 전체 결제 플로우 테스트 (더 현실적)
     */
    @GetMapping("/complete-flow")
    public String testCompletePaymentFlow() {
        try {
            log.info("🧪 전체 결제 플로우 재시도 테스트 시작");

            String testTid = "full_test_" + System.currentTimeMillis();
            String testPgToken = "pg_token_456";
            int testOrderNo = 88888;
            String testUserId = "testUser999";

            // 전체 결제 프로세스 테스트 (DB 저장까지 포함)
            // 주의: 실제 DB에 테스트 데이터가 저장될 수 있습니다!
            int payNo = paymentService.processPaymentComplete(testTid, testPgToken, testOrderNo, testUserId);

            log.info("🎉 전체 플로우 테스트 성공! PayNo: {}", payNo);
            return "✅ 전체 플로우 테스트 성공! PayNo: " + payNo;

        } catch (Exception e) {
            log.error("❌ 전체 플로우 테스트 실패", e);
            return "❌ 전체 플로우 테스트 실패: " + e.getMessage();
        }
    }
}
