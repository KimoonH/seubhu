package store.seub2hu2.payment.exception;

/**
 * 카카오페이 결제 승인 실패 예외
 */
public class KakaoPayApproveException extends KakaoPayApiException {

    public KakaoPayApproveException(String userMessage) {
        super("APPROVE", "PAYMENT_APPROVE_FAILED", userMessage);
    }

    public KakaoPayApproveException(String userMessage, Throwable cause) {
        super("APPROVE", "PAYMENT_APPROVE_FAILED", userMessage, cause);
    }
}
