package store.seub2hu2.payment.enums;

public enum PaymentMethod {

    KAKAO_PAY("카카오페이");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 문자열 값으로 Enum 찾기
     */
    public static PaymentMethod fromValue(String value) {
        for (PaymentMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown payment method: " + value);
    }

}
