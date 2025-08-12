package store.seub2hu2.payment.exception;

/**
 * 카카오페이 결제 취소 실패 예외
 */
public class KakaoPayCancelException extends KakaoPayApiException {

    public KakaoPayCancelException(String userMessage) {
        super("CANCEL", "PAYMENT_CANCEL_FAILED", userMessage);
    }

    public KakaoPayCancelException(String userMessage, Throwable cause) {
        super("CANCEL", "PAYMENT_CANCEL_FAILED", userMessage, cause);
    }
}
