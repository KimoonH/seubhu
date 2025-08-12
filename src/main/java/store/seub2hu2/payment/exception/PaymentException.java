package store.seub2hu2.payment.exception;

/**
 * 결제 관련 최상위 예외 클래스
 */
public class PaymentException extends RuntimeException {
    private final String errorCode;
    private final String userMessage;

    public PaymentException(String errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public PaymentException(String errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
