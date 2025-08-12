package store.seub2hu2.payment.exception;

/**
 * 네트워크 통신 실패 예외
 */
public class PaymentNetworkException extends PaymentException {

    public PaymentNetworkException(String userMessage) {
        super("NETWORK_ERROR", userMessage);
    }

    public PaymentNetworkException(String userMessage, Throwable cause) {
        super("NETWORK_ERROR", userMessage, cause);
    }
}
