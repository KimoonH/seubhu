package store.seub2hu2.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.seub2hu2.delivery.mapper.DeliveryMapper;
import store.seub2hu2.delivery.vo.Delivery;
import store.seub2hu2.order.exception.DatabaseSaveException;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.user.mapper.UserMapper;
import store.seub2hu2.user.vo.Addr;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryService {

    private final DeliveryMapper deliveryMapper;
    private final UserMapper userMapper;

    /**
     * 주문에 대한 배송 정보를 생성합니다.
     * @param paymentDto 결제 정보 (배송지 포함)
     * @param orderNo 주문 번호
     */
    public void createDeliveryInfo(PaymentDto paymentDto, int orderNo) {
        // 1. 배송지 주소 저장
        Addr addr = createAddress(paymentDto);

        // 2. 배송 정보 저장
        createDelivery(paymentDto, orderNo, addr.getNo());
    }

    /**
     * 배송지 주소를 저장합니다.
     */
    private Addr createAddress(PaymentDto paymentDto) {
        Addr addr = new Addr();
        addr.setName(paymentDto.getRecipientName());
        addr.setPostcode(paymentDto.getPostcode());
        addr.setAddress(paymentDto.getAddress());
        addr.setAddressDetail(paymentDto.getAddressDetail());
        addr.setUserNo(paymentDto.getUserNo());

        try {
            userMapper.insertAddress(addr);
        } catch (Exception ex) {
            throw new DatabaseSaveException("배송지 정보 저장 실패", ex);
        }

        return addr;
    }

    /**
     * 배송 상태 정보를 저장합니다.
     */
    private void createDelivery(PaymentDto paymentDto, int orderNo, int addrNo) {
        Delivery delivery = new Delivery();
        delivery.setOrderNo(orderNo);
        delivery.setAddrNo(addrNo);
        delivery.setMemo(paymentDto.getMemo());
        delivery.setDeliPhoneNumber(paymentDto.getPhoneNumber());

        try {
            deliveryMapper.insertDeliveryMemo(delivery);
        } catch (Exception ex) {
            throw new DatabaseSaveException("배송 상태 정보 저장 실패", ex);
        }
    }
}
