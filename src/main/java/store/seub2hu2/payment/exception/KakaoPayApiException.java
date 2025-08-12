package store.seub2hu2.payment.exception;

/**
 * 카카오페이 API 관련 예외
 */
public class KakaoPayApiException extends PaymentException {

    private final String apiMethod;

    public KakaoPayApiException(String apiMethod, String errorCode, String userMessage) {
        super(errorCode, userMessage);
        this.apiMethod = apiMethod;
    }

    public KakaoPayApiException(String apiMethod, String errorCode, String userMessage, Throwable cause) {
        super(errorCode, userMessage, cause);
        this.apiMethod = apiMethod;
    }

    public String getApiMethod() {
        return apiMethod;
    }
}
