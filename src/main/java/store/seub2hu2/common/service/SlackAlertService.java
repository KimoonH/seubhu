package store.seub2hu2.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlackAlertService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    @Value("${slack.alerts.enabled:true}")
    private boolean alertsEnabled;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * 결제 재시도 최종 실패 알림
     */
    public void sendPaymentRetryFailureAlert(String methodName, int maxAttempts, String tid, int orderNo, String errorMessage) {
        if (!isAlertEnabled()) {
            log.info("📢 슬랙 알림 비활성화 상태 - 로그로만 기록");
            return;
        }

        try {
            String alertTitle = "🚨 결제 재시도 최종 실패";
            String alertColor = "danger";  // 빨간색

            String message = createPaymentFailureMessage(methodName, maxAttempts, tid, orderNo, errorMessage);

            sendSlackMessage(alertTitle, message, alertColor);

            log.info("📢 슬랙 알림 발송 완료 - 결제 실패: TID {}", tid);

        } catch (Exception e) {
            log.error("📢 슬랙 알림 발송 실패", e);
            // 알림 실패해도 시스템은 계속 동작해야 함
        }
    }

    /**
     * AOP 모니터링 연속 실패 알림
     */
    public void sendConsecutiveFailureAlert(String methodName, int consecutiveFailures, String errorMessage) {
        if (!isAlertEnabled()) {
            return;
        }

        try {
            String alertTitle = "⚠️ 연속 실패 감지";
            String alertColor = "warning";  // 주황색

            String message = createConsecutiveFailureMessage(methodName, consecutiveFailures, errorMessage);

            sendSlackMessage(alertTitle, message, alertColor);

            log.info("📢 슬랙 알림 발송 완료 - 연속 실패: {} ({}번)", methodName, consecutiveFailures);

        } catch (Exception e) {
            log.error("📢 슬랙 알림 발송 실패", e);
        }
    }

    /**
     * 일반 정보 알림 (성공, 복구 등)
     */
    public void sendInfoAlert(String title, String message) {
        if (!isAlertEnabled()) {
            return;
        }

        try {
            sendSlackMessage(title, message, "good");  // 초록색
            log.info("📢 슬랙 정보 알림 발송 완료: {}", title);

        } catch (Exception e) {
            log.error("📢 슬랙 알림 발송 실패", e);
        }
    }

    /**
     * 테스트 알림 발송
     */
    public void sendTestAlert() {
        try {
            String testMessage = createTestMessage();
            sendSlackMessage("🧪 테스트 알림", testMessage, "good");
            log.info("📢 슬랙 테스트 알림 발송 완료");

        } catch (Exception e) {
            log.error("📢 슬랙 테스트 알림 발송 실패", e);
            throw new RuntimeException("슬랙 테스트 알림 발송 실패", e);
        }
    }

    /**
     * 슬랙 메시지 발송 (핵심 메서드)
     */
    private void sendSlackMessage(String title, String message, String color) throws Exception {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("📢 슬랙 Webhook URL이 설정되지 않음 - application.yml 확인 필요");
            return;
        }

        // 슬랙 메시지 포맷 구성
        Map<String, Object> slackMessage = new HashMap<>();
        slackMessage.put("text", title);
        slackMessage.put("username", "결제시스템 모니터링");
        slackMessage.put("icon_emoji", ":warning:");

        // 첨부 파일 형태로 상세 내용 추가
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", color);
        attachment.put("text", message);
        attachment.put("footer", "결제 시스템 (" + activeProfile + ")");
        attachment.put("ts", System.currentTimeMillis() / 1000);  // 타임스탬프

        slackMessage.put("attachments", new Object[]{attachment});

        // HTTP 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // JSON 변환 및 전송
        String jsonPayload = objectMapper.writeValueAsString(slackMessage);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

        restTemplate.postForEntity(webhookUrl, entity, String.class);
    }

    /**
     * 결제 실패 메시지 생성
     */
    private String createPaymentFailureMessage(String methodName, int maxAttempts, String tid, int orderNo, String errorMessage) {
        return String.format(
                """
                🚨 *결제 처리 최종 실패*
                
                📍 *상세 정보:*
                • 메서드: `%s`
                • 재시도 횟수: %d회 모두 실패
                • TID: `%s`
                • 주문번호: `%d`
                • 발생시간: %s
                
                💥 *오류 내용:*
                ```
                %s
                ```
                
                ⚡ *조치 사항:*
                • 즉시 결제 상태 확인 필요
                • 고객 문의 대응 준비
                • 수동 처리 검토 필요
                """,
                methodName,
                maxAttempts,
                tid,
                orderNo,
                getCurrentTimestamp(),
                errorMessage
        );
    }

    /**
     * 연속 실패 메시지 생성
     */
    private String createConsecutiveFailureMessage(String methodName, int consecutiveFailures, String errorMessage) {
        return String.format(
                """
                ⚠️ *연속 실패 임계값 초과*
                
                📍 *상세 정보:*
                • 메서드: `%s`
                • 연속 실패: %d회
                • 발생시간: %s
                
                💥 *최근 오류:*
                ```
                %s
                ```
                
                🔍 *권장 조치:*
                • 외부 API 상태 확인
                • 네트워크 연결 점검
                • 시스템 리소스 확인
                """,
                methodName,
                consecutiveFailures,
                getCurrentTimestamp(),
                errorMessage
        );
    }

    /**
     * 테스트 메시지 생성
     */
    private String createTestMessage() {
        return String.format(
                """
                🧪 *슬랙 알림 테스트*
                
                ✅ 결제 시스템 모니터링이 정상적으로 작동 중입니다.
                
                📊 *시스템 정보:*
                • 환경: %s
                • 테스트 시간: %s
                • 상태: 정상
                
                💡 이 메시지가 정상적으로 수신되었다면 슬랙 알림 설정이 완료되었습니다.
                """,
                activeProfile,
                getCurrentTimestamp()
        );
    }

    /**
     * 알림 활성화 여부 체크
     */
    private boolean isAlertEnabled() {
        if (!alertsEnabled) {
            log.debug("슬랙 알림이 설정에서 비활성화됨");
            return false;
        }

        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.debug("슬랙 Webhook URL이 설정되지 않음");
            return false;
        }

        return true;
    }

    /**
     * 1️⃣ 결제 승인 실패 알림 (PaymentService @Recover에서 사용)
     *
     * 사용 위치: PaymentService.sendPaymentFailureAlert() 메서드에서 호출
     * 호출 시점: 결제 승인이 3번 재시도 후 최종 실패했을 때
     */
    public void sendPaymentFailureAlert(String tid, int orderNo, String errorMessage) {
        if (!isAlertEnabled()) {
            log.info("📢 슬랙 알림 비활성화 상태 - 로그로만 기록");
            return;
        }

        try {
            String alertTitle = "🚨 결제 승인 최종 실패";
            String alertColor = "danger";  // 빨간색

            String message = String.format(
                    """
                    🚨 *결제 승인 최종 실패*
                    
                    📍 *상세 정보:*
                    • TID: `%s`
                    • 주문번호: `%d`
                    • 발생시간: %s
                    
                    💥 *오류 내용:*
                    ```
                    %s
                    ```
                    
                    ⚡ *긴급 조치 필요:*
                    • 즉시 결제 상태 확인
                    • 고객 문의 대응 준비
                    • 수동 처리 검토 필요
                    • 카카오페이 결제 내역 확인
                    """,
                    tid,
                    orderNo,
                    getCurrentTimestamp(),
                    errorMessage
            );

            sendSlackMessage(alertTitle, message, alertColor);

            log.info("📢 슬랙 알림 발송 완료 - 결제 실패: TID {}", tid);

        } catch (Exception e) {
            log.error("📢 슬랙 알림 발송 실패", e);
            // 알림 실패해도 시스템은 계속 동작해야 함
        }
    }

    /**
     * 2️⃣ 결제 취소 실패 알림 (PaymentService @Recover에서 사용)
     *
     * 사용 위치: PaymentService.recoverPaymentCancel() 메서드에서 호출
     * 호출 시점: 결제 취소가 여러 번 재시도 후 최종 실패했을 때
     */
    public void sendPaymentCancelFailureAlert(String paymentId, int amount, String errorMessage) {
        if (!isAlertEnabled()) {
            log.info("📢 슬랙 알림 비활성화 상태 - 로그로만 기록");
            return;
        }

        try {
            String alertTitle = "🚨 결제 취소 최종 실패";
            String alertColor = "danger";  // 빨간색

            String message = String.format(
                    """
                            🚨 *결제 취소 최종 실패*
                            
                            📍 *상세 정보:*
                            • PaymentID: `%s`
                            • 취소 금액: `%,d원`
                            • 발생시간: %s
                            
                            💥 *오류 내용:*
                            ```
                            %s
                            ```
                            
                            🚨 *긴급 조치 필요:*
                            • 즉시 카카오페이 관리자 페이지 확인
                            • 수동 취소 처리 검토
                            • 고객 환불 상태 점검
                            • 회계팀 통보 필요
                            """,
                    paymentId,
                    amount,
                    getCurrentTimestamp(),
                    errorMessage
            );

            sendSlackMessage(alertTitle, message, alertColor);

            log.info("📢 슬랙 알림 발송 완료 - 취소 실패: PaymentID {}", paymentId);

        } catch (Exception e) {
            log.error("📢 슬랙 알림 발송 실패", e);
            // 알림 실패해도 시스템은 계속 동작해야 함
        }
    }

    /**
     * 현재 시간 포맷팅
     */
    private String getCurrentTimestamp () {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
