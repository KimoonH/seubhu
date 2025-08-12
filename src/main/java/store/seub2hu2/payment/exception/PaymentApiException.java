package store.seub2hu2.payment.exception;

/**
 * 결제 API 호출 실패 예외
 */
public class PaymentApiException extends PaymentException {

    public PaymentApiException(String errorCode, String userMessage) {
        super(errorCode, userMessage);
    }

    public PaymentApiException(String errorCode, String userMessage, Throwable cause) {
        super(errorCode, userMessage, cause);
    }
}
