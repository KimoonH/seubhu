package store.seub2hu2.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.seub2hu2.delivery.service.DeliveryService;
import store.seub2hu2.mypage.service.CartService;
import store.seub2hu2.order.service.OrderService;
import store.seub2hu2.order.vo.Order;
import store.seub2hu2.order.vo.OrderItem;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.dto.CancelResponse;
import store.seub2hu2.payment.dto.PaymentReadyResponse;

import store.seub2hu2.payment.exception.PaymentValidationException;
import store.seub2hu2.payment.strategy.PaymentProcessor;
import store.seub2hu2.product.service.ProductService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static store.seub2hu2.payment.constant.PaymentConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService {

    @Value("${server.ip}")
    private String serverIp;

    @Value("${kakaopay.cid}")
    private String cid;

    @Value("${kakaopay.partner-user-id}")
    private String partnerUserId;

    private final List<PaymentProcessor> paymentProcessors;

    private final KakaoPayApiService kakaoPayApiService;

    // 카카오페이 결제 승인
    // 사용자가 결제 수단을 선택하고 비밀번호를 입력해 결제 인증을 완료한 뒤,
    // 최종적으로 결제 완료 처리를 하는 단계
    @Transactional
    public PaymentReadyResponse payReady(PaymentDto paymentDto) {
        validatePaymentDto(paymentDto);

        log.info("Pay ready dto = {}", paymentDto);
        Map<String, String> parameters = createBaseParameters(paymentDto);
        // 상품 결제
        processPaymentByStrategy(paymentDto, parameters);


        parameters.put("tax_free_amount", TAX_FREE_AMOUNT);
        parameters.put("cancel_url", serverIp + CANCEL_URL_PATH);
        parameters.put("fail_url", serverIp + FAIL_URL_PATH);


        log.info("=== 결제준비 partner_order_id: {}", parameters.get("partner_order_id"));
        return kakaoPayApiService.requestPaymentReady(parameters);
    }


    @NotNull
    private Map<String, String> createBaseParameters(PaymentDto paymentDto) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("cid", cid);
        parameters.put("partner_user_id", partnerUserId);
        parameters.put("quantity", String.valueOf(paymentDto.getQuantity()));
        return parameters;
    }

    // 카카오페이 결제 승인
    // 사용자가 결제 수단을 선택하고 비밀번호를 입력해 결제 인증을 완료한 뒤,
    // 최종적으로 결제 완료 처리를 하는 단계
    public ApproveResponse payApprove(String tid, String pgToken, int orderNo) {
        validateApproveParameters(tid, pgToken, orderNo);
        log.info("결제 승인 요청 - TID: {}, OrderNo: {}", tid, orderNo);

        return kakaoPayApiService.requestPaymentApprove(tid, pgToken, String.valueOf(orderNo));
    }

    // 카카오페이 결제 취소
    // 사용자가 결제 수단을 선택하고 비밀번호를 입력해 결제 인증을 완료한 뒤,
    // 최종적으로 결제 취소 처리를 하는 단계
    public CancelResponse payCancel(PaymentDto paymentDto, String tid) {
        validateCancelParameters(paymentDto, tid);
        log.info("결제 취소 요청 - TID: {}, Amount: {}", tid, paymentDto.getTotalAmount());

        return kakaoPayApiService.requestPaymentCancel(
                tid,
                String.valueOf(paymentDto.getTotalAmount()),
                String.valueOf(paymentDto.getQuantity())
        );
    }

    /**
     * PaymentDto 유효성 검증
     */
    private void validatePaymentDto(PaymentDto paymentDto) {
        if (paymentDto == null) {
            throw new PaymentValidationException("결제 정보가 없습니다.");
        }

        if (paymentDto.getType() == null || paymentDto.getType().trim().isEmpty()) {
            throw new PaymentValidationException("결제 타입이 지정되지 않았습니다.");
        }

        if (paymentDto.getQuantity() <= 0) {  //
            throw new PaymentValidationException("주문 수량은 0보다 커야 합니다.");
        }

        if (paymentDto.getFinalTotalPrice() <= 0) {
            throw new PaymentValidationException("결제 금액은 0보다 커야 합니다.");
        }
    }

    /**
     * 결제 승인 파라미터 유효성 검증
     */
    private void validateApproveParameters(String tid, String pgToken, int orderNo) {
        if (tid == null || tid.trim().isEmpty()) {
            throw new PaymentValidationException("TID가 없습니다.");
        }

        if (pgToken == null || pgToken.trim().isEmpty()) {
            throw new PaymentValidationException("PG Token이 없습니다.");
        }

        if (orderNo <= 0) {
            throw new PaymentValidationException("주문번호가 유효하지 않습니다.");
        }
    }

    /**
     * 결제 취소 파라미터 유효성 검증
     */
    private void validateCancelParameters(PaymentDto paymentDto, String tid) {
        if (tid == null || tid.trim().isEmpty()) {
            throw new PaymentValidationException("TID가 없습니다.");
        }

        if (paymentDto == null) {
            throw new PaymentValidationException("결제 정보가 없습니다.");
        }

        if (paymentDto.getTotalAmount() <= 0) {
            throw new PaymentValidationException("취소할 금액이 유효하지 않습니다.");
        }

        if (paymentDto.getQuantity() <= 0) {
            throw new PaymentValidationException("취소할 수량이 유효하지 않습니다.");
        }
    }

    /**
     * 결제 타입에 맞는 전략을 찾아서 실행
     */
    private void processPaymentByStrategy(PaymentDto paymentDto, Map<String, String> parameters) {
        String paymentType = paymentDto.getType();

        PaymentProcessor processor = paymentProcessors.stream()
                .filter(p -> p.supports(paymentType))
                .findFirst()
                .orElseThrow(() -> new PaymentValidationException("지원하지 않는 결제 타입입니다: " + paymentType));

        processor.processPayment(paymentDto, parameters);
    }

}