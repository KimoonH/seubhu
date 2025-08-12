package store.seub2hu2.payment.enums;

public enum PaymentType {

    PRODUCT("상품");

    private final String value;

    PaymentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 문자열 값으로 Enum 찾기
     */
    public static PaymentType fromValue(String value) {
        for (PaymentType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown payment type: " + value);
    }


}
