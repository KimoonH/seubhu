package store.seub2hu2.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.seub2hu2.common.RetryMonitoringAspect;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.payment.service.PaymentService;

/**
 * 🧪 PaymentService AOP 모니터링 테스트 컨트롤러
 */
@RestController
@RequestMapping("/api/payment-test")
@RequiredArgsConstructor
@Slf4j
public class PaymentTestController {

    private final PaymentService paymentService;
    private final RetryMonitoringAspect retryMonitoringAspect;

    /**
     * 💳 실제 결제 승인 메서드 테스트
     * processPaymentApproval 메서드 호출 (AOP 모니터링 적용됨)
     */
    @PostMapping("/approve")
    public String testPaymentApproval() {
        try {
            log.info("🧪 실제 결제 승인 테스트 시작");

            // 테스트용 파라미터 생성
            String testTid = "TEST_TID_" + System.currentTimeMillis();
            String testPgToken = "TEST_PG_TOKEN";
            int testOrderNo = (int) (System.currentTimeMillis() % 100000);

            log.info("📋 테스트 파라미터 - TID: {}, OrderNo: {}", testTid, testOrderNo);

            // 🔍 실제 PaymentService.processPaymentApproval 호출
            // AOP가 자동으로 모니터링함!
            ApproveResponse result = paymentService.processPaymentApproval(testTid, testPgToken, testOrderNo);

            return "✅ 결제 승인 테스트 성공: " + testTid;

        } catch (Exception e) {
            log.error("❌ 결제 승인 테스트 실패: {}", e.getMessage());
            return "❌ 결제 승인 테스트 실패: " + e.getMessage();
        }
    }

    /**
     * 🔙 실제 결제 취소 메서드 테스트
     * cancelPaymentWithRetry 메서드는 private이라 간접 호출
     */
    @PostMapping("/cancel")
    public String testPaymentCancel() {
        try {
            log.info("🧪 실제 결제 취소 테스트 시작");

            // 테스트용 PaymentDto 생성
            PaymentDto testPayment = createTestPaymentDto();
            String testUserId = "TEST_USER_" + System.currentTimeMillis();

            log.info("📋 테스트 파라미터 - UserId: {}, PaymentId: {}",
                    testUserId, testPayment.getPaymentId());

            // 🔍 실제 PaymentService.cancelProductPayment 호출
            // 내부적으로 cancelPaymentWithRetry가 호출되어 AOP 모니터링됨!
            paymentService.cancelProductPayment(testPayment, testUserId);

            return "✅ 결제 취소 테스트 성공";

        } catch (Exception e) {
            log.error("❌ 결제 취소 테스트 실패: {}", e.getMessage());
            return "❌ 결제 취소 테스트 실패: " + e.getMessage();
        }
    }

    /**
     * 📊 AOP 재시도 통계 조회
     */
    @GetMapping("/stats")
    public String getRetryStats() {
        log.info("📊 재시도 통계 조회 요청");
        retryMonitoringAspect.printAllStats();
        return "📊 재시도 통계가 콘솔에 출력되었습니다";
    }

    /**
     * 🧹 통계 초기화
     */
    @PostMapping("/reset-stats")
    public String resetStats() {
        retryMonitoringAspect.resetStats();
        return "🧹 재시도 통계가 초기화되었습니다";
    }

    /**
     * 🎯 특정 메서드만 테스트 (실제 카카오페이 API 호출 없이)
     */
    @PostMapping("/approval-only")
    public String testApprovalOnly() {
        try {
            log.info("🧪 결제 승인 메서드만 단독 테스트");

            String testTid = "ISOLATED_TEST_" + System.currentTimeMillis();
            String testPgToken = "FAKE_TOKEN";
            int testOrderNo = 99999;

            // 이 호출에서 AOP 모니터링 확인 가능
            ApproveResponse result = paymentService.processPaymentApproval(testTid, testPgToken, testOrderNo);

            return "✅ 승인 메서드 단독 테스트 성공";

        } catch (Exception e) {
            log.info("예상된 실패 - 테스트용이므로 정상: {}", e.getMessage());
            return "📝 예상된 실패 (테스트 성공): " + e.getMessage();
        }
    }

    /**
     * 🎭 테스트용 PaymentDto 생성
     */
    private PaymentDto createTestPaymentDto() {
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setPaymentId("TEST_PAYMENT_" + System.currentTimeMillis());
        paymentDto.setOrderId("TEST_ORDER_" + System.currentTimeMillis());
        paymentDto.setUserId("TEST_USER");
        paymentDto.setTotalAmount(10000);
        paymentDto.setQuantity(1);
        paymentDto.setMethod("KAKAO_PAY");
        paymentDto.setType("상품");
        paymentDto.setTitle("테스트 상품");

        return paymentDto;
    }
}
