package store.seub2hu2.common.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import store.seub2hu2.common.service.SlackAlertService;

@RestController
@RequestMapping("/api/slack-test")
@RequiredArgsConstructor
@Slf4j
public class SlackTestController {

    private final SlackAlertService slackAlertService;

    /**
     * 🧪 가장 기본적인 테스트 알림
     * 기존 SlackAlertService.sendTestAlert() 사용
     */
    @GetMapping("/test")
    public String sendTestAlert() {
        log.info("🧪 슬랙 테스트 알림 발송 요청");

        try {
            // 기존에 구현된 sendTestAlert() 메서드 사용
            slackAlertService.sendTestAlert();

            return "✅ 슬랙 테스트 알림이 발송되었습니다! 슬랙 채널을 확인해보세요.";

        } catch (Exception e) {
            log.error("❌ 슬랙 테스트 실패", e);
            return "❌ 슬랙 테스트 실패: " + e.getMessage();
        }
    }

    /**
     * 🎭 간단한 정보 알림 테스트
     * 기존 SlackAlertService.sendInfoAlert() 사용
     */
    @PostMapping("/info")
    public String sendInfoAlert(@RequestParam(defaultValue = "테스트 알림") String message) {
        log.info("📢 정보 알림 발송 요청: {}", message);

        try {
            slackAlertService.sendInfoAlert("🧪 테스트", message);

            return "✅ 정보 알림 발송 완료: " + message;

        } catch (Exception e) {
            log.error("❌ 정보 알림 발송 실패", e);
            return "❌ 정보 알림 발송 실패: " + e.getMessage();
        }
    }

    /**
     * 🚨 결제 실패 알림 테스트 (실제 상황 시뮬레이션)
     */
    @PostMapping("/payment-failure")
    public String testPaymentFailureAlert() {
        log.info("🚨 결제 실패 알림 테스트");

        try {
            // 테스트용 결제 실패 상황 시뮬레이션
            slackAlertService.sendPaymentRetryFailureAlert(
                    "PaymentService.approvePayment",  // 메서드명
                    3,                                 // 최대 재시도 횟수
                    "TEST_TID_" + System.currentTimeMillis(), // 테스트 TID
                    99999,                            // 테스트 주문번호
                    "테스트용 결제 실패 - 카카오페이 API 오류"  // 에러 메시지
            );

            return "🚨 결제 실패 알림 테스트 완료! (실제 결제와는 무관한 테스트입니다)";

        } catch (Exception e) {
            log.error("❌ 결제 실패 알림 테스트 실패", e);
            return "❌ 결제 실패 알림 테스트 실패: " + e.getMessage();
        }
    }

    /**
     * 🔍 현재 슬랙 알림 설정 상태 확인
     */
    @GetMapping("/status")
    public String checkSlackStatus() {
        return """
            📊 슬랙 알림 시스템 테스트
            
            🧪 테스트 방법:
            1. GET  /api/slack-test/test                    - 기본 테스트 알림
            2. POST /api/slack-test/info?message=안녕하세요   - 정보 알림 테스트  
            3. POST /api/slack-test/payment-failure         - 결제 실패 알림 테스트
            
            ⚙️ 설정 확인 사항:
            - application.yml에서 slack.webhook.url 설정
            - slack.alerts.enabled=true 설정
            - RestTemplate Bean 등록 여부
            
            📝 참고:
            - URL이 설정되지 않으면 로그로만 출력됩니다
            - 실제 슬랙 채널에서 알림을 확인하세요
            """;
    }
}
