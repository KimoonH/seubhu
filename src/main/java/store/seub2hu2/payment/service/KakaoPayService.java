package store.seub2hu2.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.seub2hu2.delivery.service.DeliveryService;
import store.seub2hu2.mypage.service.CartService;
import store.seub2hu2.order.exception.*;
import store.seub2hu2.order.service.OrderService;
import store.seub2hu2.order.vo.Order;
import store.seub2hu2.order.vo.OrderItem;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.dto.CancelResponse;
import store.seub2hu2.payment.dto.PaymentReadyResponse;

import store.seub2hu2.product.service.ProductService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService {

    @Value("${server.ip}")
    private String serverIp;

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
        log.info("Pay ready dto = {}", paymentDto);
        Map<String, String> parameters = createBaseParameters(paymentDto);


        // 상품 결제
        if (paymentDto.getType().equals("상품")) {
            Order order = orderService.createOrder(paymentDto);
            int orderNo = order.getNo();

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
            parameters.put("approval_url", serverIp + "/pay/completed?type=" + paymentDto.getType()
                    + "&orderNo=" + orderNo);

        }
        parameters.put("tax_free_amount", "0");
        parameters.put("cancel_url", serverIp + "/pay/cancel");
        parameters.put("fail_url", serverIp + "/pay/fail");



        return kakaoPayApiService.requestPaymentReady(parameters);
    }

    @NotNull
    private Map<String, String> createBaseParameters(PaymentDto paymentDto) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("cid", "TC0ONETIME");
        parameters.put("partner_order_id", "1234567890");
        parameters.put("partner_user_id", "seub2hu2");
        parameters.put("quantity", String.valueOf(paymentDto.getQuantity()));
        return parameters;
    }

    // 카카오페이 결제 승인
    // 사용자가 결제 수단을 선택하고 비밀번호를 입력해 결제 인증을 완료한 뒤,
    // 최종적으로 결제 완료 처리를 하는 단계
    public ApproveResponse payApprove(String tid, String pgToken, int orderNo) {
        return kakaoPayApiService.requestPaymentApprove(tid, pgToken);
    }

    // 카카오페이 결제 취소
    // 사용자가 결제 수단을 선택하고 비밀번호를 입력해 결제 인증을 완료한 뒤,
    // 최종적으로 결제 취소 처리를 하는 단계
    public CancelResponse payCancel(PaymentDto paymentDto, String tid) {
        return kakaoPayApiService.requestPaymentCancel(
                tid,
                String.valueOf(paymentDto.getTotalAmount()),
                String.valueOf(paymentDto.getQuantity())
        );
    }
}