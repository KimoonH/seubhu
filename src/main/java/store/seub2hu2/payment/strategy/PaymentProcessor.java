package store.seub2hu2.payment.strategy;

import store.seub2hu2.payment.dto.PaymentDto;

import java.util.Map;

public interface PaymentProcessor {

    /**
     * 지원하는 결제 타입인지 확인
     */
    boolean supports(String paymentType);

    /**
     * 결제 처리 로직 실행
     */
    void processPayment(PaymentDto paymentDto, Map<String, String> parameters);
}
