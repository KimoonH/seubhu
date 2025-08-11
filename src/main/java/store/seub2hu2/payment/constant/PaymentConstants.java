package store.seub2hu2.payment.constant;

public final class PaymentConstants {

    // 생성자를 private으로 막아서 인스턴스 생성 방지
    private PaymentConstants() {
        throw new AssertionError("상수 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    // ========== 공통 상수 ==========

    // 결제 타입 상수
    public static final String PAYMENT_TYPE_PRODUCT = "상품";

    // 결제 관련 상수
    public static final String TAX_FREE_AMOUNT = "0";

    // URL 경로 상수
    public static final String APPROVAL_URL_PATH = "/pay/completed";
    public static final String CANCEL_URL_PATH = "/pay/cancel";
    public static final String FAIL_URL_PATH = "/pay/fail";

    // ========== 결제 수단별 상수 ==========

    /**
     * 카카오페이 관련 상수
     */
    public static final class KakaoPay {
        // HTTP 헤더
        public static final String AUTHORIZATION_HEADER = "Authorization";
        public static final String CONTENT_TYPE_HEADER = "Content-type";

        // 헤더 값
        public static final String AUTHORIZATION_PREFIX = "SECRET_KEY ";
        public static final String CONTENT_TYPE_JSON = "application/json";

        // API 파라미터 키
        public static final String PARAM_CID = "cid";
        public static final String PARAM_TID = "tid";
        public static final String PARAM_PARTNER_ORDER_ID = "partner_order_id";
        public static final String PARAM_PARTNER_USER_ID = "partner_user_id";
        public static final String PARAM_PG_TOKEN = "pg_token";
        public static final String PARAM_CANCEL_AMOUNT = "cancel_amount";
        public static final String PARAM_TAX_FREE_AMOUNT = "tax_free_amount";  // 결제준비용
        public static final String PARAM_CANCEL_TAX_FREE_AMOUNT = "cancel_tax_free_amount";
        public static final String PARAM_QUANTITY = "quantity";
        public static final String PARAM_CANCEL_URL = "cancel_url";
        public static final String PARAM_FAIL_URL = "fail_url";
        public static final String PARAM_APPROVAL_URL = "approval_url";
        public static final String PARAM_ITEM_NAME = "item_name";
        public static final String PARAM_TOTAL_AMOUNT = "total_amount";

        private KakaoPay() {}
    }
}
