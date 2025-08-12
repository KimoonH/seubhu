package store.seub2hu2.payment.exception;

/**
 * 결제 처리 중 일반적인 실패 예외
 */
public class PaymentProcessingException extends PaymentException {

    public PaymentProcessingException(String message) {
        super("PROCESSING_FAILED", message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super("PROCESSING_FAILED", message, cause);
    }
}
