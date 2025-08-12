package store.seub2hu2.payment.exception;

/**
 * 카카오페이 결제 준비 실패 예외
 */
public class KakaoPayReadyException extends KakaoPayApiException {

    public KakaoPayReadyException(String userMessage) {
        super("READY", "PAYMENT_READY_FAILED", userMessage);
    }

    public KakaoPayReadyException(String userMessage, Throwable cause) {
        super("READY", "PAYMENT_READY_FAILED", userMessage, cause);
    }
}
