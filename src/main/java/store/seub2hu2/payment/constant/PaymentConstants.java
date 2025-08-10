package store.seub2hu2.payment.constant;

public final class PaymentConstants {

    // 생성자를 private으로 막아서 인스턴스 생성 방지
    private PaymentConstants() {
        throw new AssertionError("상수 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    // 결제 타입 상수
    public static final String PAYMENT_TYPE_PRODUCT = "상품";

    // 결제 관련 상수
    public static final String TAX_FREE_AMOUNT = "0";

    // URL 경로 상수
    public static final String APPROVAL_URL_PATH = "/pay/completed";
    public static final String CANCEL_URL_PATH = "/pay/cancel";
    public static final String FAIL_URL_PATH = "/pay/fail";
}
