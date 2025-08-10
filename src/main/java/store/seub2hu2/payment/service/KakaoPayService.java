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



    private final KakaoPayApiService kakaoPayApiService;

    private final ProductService productService;

    private final OrderService orderService;

    private final CartService cartService;

    private final DeliveryService deliveryService;

    // 카카오페이 결제 승인
    // 사용자가 결제 수단을 선택하고 비밀번호를 입력해 결제 인증을 완료한 뒤,
    // 최종적으로 결제 완료 처리를 하는 단계
    @Transactional
    public PaymentReadyResponse payReady(PaymentDto paymentDto) {
        validatePaymentDto(paymentDto);

        log.info("Pay ready dto = {}", paymentDto);
        Map<String, String> parameters = createBaseParameters(paymentDto);
        // 상품 결제
        if (paymentDto.getType().equals(PAYMENT_TYPE_PRODUCT)) {
            processProductPayment(paymentDto, parameters);

        }
        parameters.put("tax_free_amount", TAX_FREE_AMOUNT);
        parameters.put("cancel_url", serverIp + CANCEL_URL_PATH);
        parameters.put("fail_url", serverIp + FAIL_URL_PATH);


        log.info("=== 결제준비 partner_order_id: {}", parameters.get("partner_order_id"));
        return kakaoPayApiService.requestPaymentReady(parameters);
    }

    /**
     * 상품 결제 프로세스 처리
     */
    private void processProductPayment(PaymentDto paymentDto, Map<String, String> parameters) {
        Order order = orderService.createOrder(paymentDto);
        int orderNo = order.getNo();


        parameters.put("partner_order_id", String.valueOf(orderNo));

        // 주문 상품 정보를 저장한다.
        List<OrderItem> orderItems = paymentDto.getOrderItems();

        String itemName = productService.generateOrderItemName(orderItems);

        // 재고 확인 및 업데이트
        productService.validateAndUpdateStock(orderItems, orderNo, itemName);

        // 장바구니에서 주문된 상품들 제거
        cartService.removeOrderedItems(orderItems);


        orderService.saveOrderItems(orderItems);

        // 배송 정보 생성
        deliveryService.createDeliveryInfo(paymentDto, orderNo);

        // 결제준비
        parameters.put("item_name", itemName);
        parameters.put("item_code", String.valueOf(orderNo));
        parameters.put("total_amount", String.valueOf(paymentDto.getFinalTotalPrice()));
        parameters.put("approval_url", buildApprovalUrl(paymentDto.getType(), orderNo));
    }

    /**
     * 승인 완료 URL 생성
     */
    private String buildApprovalUrl(String type, int orderNo) {
        return serverIp + APPROVAL_URL_PATH + "?type=" + type + "&orderNo=" + orderNo;
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

        if (paymentDto.getQuantity() <= 0) {
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
}