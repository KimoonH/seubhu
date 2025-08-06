package store.seub2hu2.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.seub2hu2.cart.dto.CartItemDto;
import store.seub2hu2.delivery.mapper.DeliveryMapper;
import store.seub2hu2.delivery.vo.Delivery;
import store.seub2hu2.mypage.dto.*;
import store.seub2hu2.order.dto.OrderForm;
import store.seub2hu2.order.exception.DatabaseSaveException;
import store.seub2hu2.order.mapper.OrderMapper;
import store.seub2hu2.order.vo.Order;
import store.seub2hu2.order.vo.OrderItem;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.product.mapper.ProductMapper;
import store.seub2hu2.product.vo.Product;
import store.seub2hu2.product.vo.Size;
import store.seub2hu2.user.vo.Addr;
import store.seub2hu2.user.vo.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    ProductMapper productMapper;

    @Autowired
    DeliveryMapper  deliveryMapper;


    /**
     * 장바구니에서 선택한 주문 상품을 조회한다.
     * @param sizeNoList 상품 사이즈 번호(고유번호)
     * @param stocks 수량들
     * @return
     */
    public List<CartItemDto> getOrderItemBySizeNo(List<Integer> sizeNoList, List<Integer> stocks, int userNo) {



        Map <Integer, Integer> map = new HashMap<>();
        for(int i = 0; i < sizeNoList.size(); i++){
            map.put(sizeNoList.get(i), stocks.get(i));
        }



        List<CartItemDto> orderDto = orderMapper.getOrderItemBySizeNo(sizeNoList, userNo);
        for(CartItemDto cartItemDto : orderDto){
            cartItemDto.setStock(map.get(cartItemDto.getSize().getNo()));
        }

        return orderDto;
    }

    public List<OrderResponse> getAllOrders(int userNo) {

            // 주문 목록 가져오기
            List<OrderResponse> orders = orderMapper.getOrders(userNo);

            // 주문 데이터를 그룹화: Map<orderNo, List<OrderResponse>>
            Map<Integer, List<OrderResponse>> groupedOrders = orders.stream()
                    .collect(Collectors.groupingBy(OrderResponse::getOrderNo));

            // 그룹화된 데이터를 가공하여 최종 List<OrderResponse> 생성
            List<OrderResponse> processedOrders = new ArrayList<>();

            for (Map.Entry<Integer, List<OrderResponse>> entry : groupedOrders.entrySet()) {
                List<OrderResponse> orderGroup = entry.getValue();

                // 첫 번째 상품 정보 가져오기
                OrderResponse firstOrder = orderGroup.get(0);

                // 상품명 가공
                if (orderGroup.size() > 1) {
                    String updatedProductName = firstOrder.getProductName() + " 외 " + (orderGroup.size() - 1) + " 개";
                    firstOrder.setProductName(updatedProductName);
                }

                // 총 수량 계산
                int totalQuantity = orderGroup.stream()
                        .mapToInt(OrderResponse::getQuantity)
                        .sum();

                // 총 가격 계산
                int totalPrice = orderGroup.stream()
                        .mapToInt(order -> order.getQuantity() * order.getProductPrice())
                        .sum();

                // 첫 번째 OrderResponse에 총 수량과 총 가격 설정
                firstOrder.setQuantity(totalQuantity); // 총 수량
                firstOrder.setProductPrice(totalPrice); // 총 가격

                // 가공된 주문을 최종 리스트에 추가
                processedOrders.add(firstOrder);
            }
            return processedOrders;
    }

    public ResponseDTO getOrderDetails(int orderNo){
        System.out.println("테스트"+orderMapper.getOrderDetails(orderNo));
        return orderMapper.getOrderDetails(orderNo);
    }

    public OrderResultDto getOrderResult(int orderNo){

        return orderMapper.getOrderResult(orderNo);
    }

    public int updateOrderPayNo(int orderNo, int payNo){
        OrdersDTO ordersDTO = new OrdersDTO();
        ordersDTO.setOrderNo(orderNo);
        ordersDTO.setPayNo(payNo);

        return orderMapper.updateOrder(ordersDTO);
    }

    // 주문 취소
    @Transactional
    public OrderResultDto cancelOrder(PaymentDto paymentDto) {

        OrderResultDto dto = this.getOrderResult(paymentDto.getOrderNo());

        // 재고 변경 완료
        List<OrderResultItemDto> items = dto.getItems();
        for (OrderResultItemDto itemDto : items) {
            Size size = productMapper.getSizeAmount(itemDto.getSizeNo());
            // 주문된 수량만큼 재고 복원
            int updatedAmount = size.getAmount() + itemDto.getOrderProdAmount();
            size.setAmount(updatedAmount);

            // 변경된 재고 업데이트
            productMapper.updateAmount(size);
        }
        
        // 주문 상태 변경
        Order order = new Order();
        order.setNo(paymentDto.getOrderNo());
        order.setStatus("주문취소");
        orderMapper.updateOrderStatus(order);

        // 배송 상태 변경
        Delivery delivery = new Delivery();
        delivery.setOrderNo(paymentDto.getOrderNo());
        delivery.setStatus("배송취소");
        deliveryMapper.updateDeliveryStatus(delivery);

        return dto;
    }

    /**
     * 새로운 주문을 생성하고 저장합니다.
     * @param paymentDto 결제 정보
     * @return 생성된 주문 객체 (자동 생성된 orderNo 포함)
     */
    public Order createOrder(PaymentDto paymentDto) {
        Order order = new Order();
        order.setTotalPrice(paymentDto.getTotalPrice());
        order.setDeliveryPrice(paymentDto.getDeliveryPrice());
        order.setDiscountPrice(paymentDto.getDiscountPrice());
        order.setFinalTotalPrice(paymentDto.getFinalTotalPrice());
        order.setUserNo(paymentDto.getUserNo());
        order.setOrderId(new OrderResultDto().generateOrderId());

        try {
            orderMapper.insertOrders(order);
        } catch (Exception ex) {
            throw new DatabaseSaveException("주문 정보 저장 실패", ex);
        }

        return order;
    }

    /**
     * 주문 상품들을 저장합니다.
     * @param orderItems 주문 상품 목록
     */
    public void saveOrderItems(List<OrderItem> orderItems) {
        try {
            orderMapper.insertOrderItems(orderItems);
        } catch (Exception ex) {
            throw new DatabaseSaveException("주문 상품 정보 저장 실패", ex);
        }
    }
}
