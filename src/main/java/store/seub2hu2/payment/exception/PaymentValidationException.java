package store.seub2hu2.payment.exception;

/**
 * 결제 유효성 검증 실패 예외
 */
public class PaymentValidationException extends PaymentException {

    public PaymentValidationException(String message) {
        super("VALIDATION_FAILED", message);
    }

    public PaymentValidationException(String message, Throwable cause) {
        super("VALIDATION_FAILED", message, cause);
    }

}
