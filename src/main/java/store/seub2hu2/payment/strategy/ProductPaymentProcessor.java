package store.seub2hu2.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import store.seub2hu2.delivery.service.DeliveryService;
import store.seub2hu2.mypage.service.CartService;
import store.seub2hu2.order.service.OrderService;
import store.seub2hu2.order.vo.Order;
import store.seub2hu2.order.vo.OrderItem;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.product.service.ProductService;

import java.util.List;
import java.util.Map;

import static store.seub2hu2.payment.constant.PaymentConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPaymentProcessor implements  PaymentProcessor {

    @Value("${server.ip}")
    private String serverIp;

    private final ProductService productService;
    private final OrderService orderService;
    private final CartService cartService;
    private final DeliveryService deliveryService;

    @Override
    public boolean supports(String paymentType) {
        return PAYMENT_TYPE_PRODUCT.equals(paymentType);
    }

    @Override
    public void processPayment(PaymentDto paymentDto, Map<String, String> parameters) {
        log.info("상품 결제 프로세스 시작 - 주문수량: {}", paymentDto.getQuantity());

        // 주문 생성
        Order order = orderService.createOrder(paymentDto);
        int orderNo = order.getNo();
        parameters.put("partner_order_id", String.valueOf(orderNo));

        // 주문 상품 정보 처리
        List<OrderItem> orderItems = paymentDto.getOrderItems();
        int totalQuantity = orderItems.stream()
                .mapToInt(OrderItem::getStock)
                .sum();

        log.info("상품 결제 프로세스 시작 - 주문상품수: {}, 총수량: {}, 총금액: {}",
                orderItems.size(), totalQuantity, paymentDto.getFinalTotalPrice());
        String itemName = productService.generateOrderItemName(orderItems);

        // 재고 확인 및 업데이트
        productService.validateAndUpdateStock(orderItems, orderNo, itemName);

        // 장바구니에서 주문된 상품들 제거
        cartService.removeOrderedItems(orderItems);

        // 주문 상품 저장
        orderService.saveOrderItems(orderItems);

        // 배송 정보 생성
        deliveryService.createDeliveryInfo(paymentDto, orderNo);

        // 결제 파라미터 설정
        setPaymentParameters(parameters, paymentDto, itemName, orderNo);

        log.info("상품 결제 프로세스 완료 - OrderNo: {}", orderNo);
    }

    private void setPaymentParameters(Map<String, String> parameters, PaymentDto paymentDto,
                                      String itemName, int orderNo) {
        parameters.put("item_name", itemName);
        parameters.put("item_code", String.valueOf(orderNo));
        parameters.put("total_amount", String.valueOf(paymentDto.getFinalTotalPrice()));
        parameters.put("approval_url", buildApprovalUrl(paymentDto.getType(), orderNo));
    }

    private String buildApprovalUrl(String type, int orderNo) {
        return serverIp + APPROVAL_URL_PATH + "?type=" + type + "&orderNo=" + orderNo;
    }
}
