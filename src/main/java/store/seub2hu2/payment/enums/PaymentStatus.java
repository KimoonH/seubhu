package store.seub2hu2.payment.enums;

/**
 * 결제 상태 Enum
 */
public enum PaymentStatus {

    PENDING("결제대기"),
    COMPLETED("결제완료"),
    CANCELLED("취소"),
    FAILED("결제실패"),
    PROCESSING_FAILED("처리실패"),
    REFUNDED("환불완료");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 문자열 값으로 Enum 찾기
     */
    public static PaymentStatus fromValue(String value) {
        for (PaymentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown payment status: " + value);
    }

    /**
     * 결제 완료 상태인지 확인
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * 취소 가능한 상태인지 확인
     */
    public boolean isCancellable() {
        return this == COMPLETED || this == PENDING;
    }
}
