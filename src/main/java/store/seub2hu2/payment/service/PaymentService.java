package store.seub2hu2.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.seub2hu2.mypage.dto.OrderResultDto;
import store.seub2hu2.order.service.OrderService;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.dto.CancelResponse;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.payment.mapper.PayMapper;
import store.seub2hu2.payment.vo.Payment;
import store.seub2hu2.security.user.LoginUser;
import store.seub2hu2.util.SessionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final KakaoPayService kakaoPayService;
    private final OrderService orderService;
    private final PayMapper payMapper;
    private final SessionUtils sessionUtils;

    public PaymentDto getPaymentById(String id) {
        Payment payment = payMapper.getPaymentById(id);
        return new PaymentDto(payment);
    }

    public String getPaymentTypeById(String id) {
        return payMapper.getPaymentTypeById(id);
    }

    public void completeLessonPayment() {
        payMapper.updateLessonPayStatus();
    }

    /**
     * Payment 객체 생성
     */
    public Payment createPayment(String tid, String userId, ApproveResponse approveResponse) {
        Payment payment = new Payment();
        payment.setId(tid);
        payment.setUserId(userId);
        payment.setStatus("결제완료");
        payment.setType("상품");
        payment.setMethod("카카오페이");
        payment.setAmount(1);
        payment.setPrice(approveResponse.getAmount().getTotal());

        log.info("Payment 객체 생성 - tid: {}, amount: {}", tid, approveResponse.getAmount().getTotal());
        return payment;
    }

    /**
     * 결제 승인부터 DB 저장까지 통합 처리
     */
    @Transactional
    public int processPaymentComplete(String tid, String pgToken, int orderNo, String userId) {
        try {
            log.info("결제 완료 처리 시작 - tid: {}, orderNo: {}", tid, orderNo);

            // 1. 카카오페이 결제 승인
            ApproveResponse approveResponse = kakaoPayService.payApprove(tid, pgToken, orderNo);

            // 2. Payment 객체 생성
            Payment payment = createPayment(tid, userId, approveResponse);

            // 3. DB에 결제 정보 저장
            payMapper.insertPay(payment);
            int payNo = payment.getNo();

            // 4. 주문에 결제 번호 연결
            orderService.updateOrderPayNo(orderNo, payNo);

            log.info("결제 완료 처리 성공 - orderNo: {}, payNo: {}", orderNo, payNo);
            return payNo;

        } catch (Exception e) {
            log.error("결제 완료 처리 실패 - orderNo: {}", orderNo, e);
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 결제 완료 전 검증
     */
    public void validatePaymentCompletion(LoginUser loginUser, String tid, int orderNo) {
        // 사용자 검증
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        // 세션 검증
        if (tid == null) {
            throw new IllegalArgumentException("결제 세션이 만료되었습니다.");
        }

        // 주문번호 검증
        if (orderNo <= 0) {
            throw new IllegalArgumentException("잘못된 주문번호입니다.");
        }

        log.info("결제 완료 검증 통과 - 사용자: {}, orderNo: {}", loginUser.getId(), orderNo);
    }

    public void clearPaymentSession() {
        sessionUtils.removeAttribute("tid");
        sessionUtils.removeAttribute("paymentUserId");
        sessionUtils.removeAttribute("paymentType");
        log.info("결제 세션 정보 정리 완료");
    }

    @Transactional
    public void cancelProductPayment(PaymentDto paymentDto, String userId) {
        try {
            log.info("상품 결제 취소 시작 - 사용자: {}", userId);

            OrderResultDto cancelResult = orderService.cancelOrder(paymentDto);
            String paymentId = cancelResult.getPayId();
            paymentDto.setTotalAmount(cancelResult.getPayPrice());

            // 카카오페이 결제 취소 (응답은 로그만)
            kakaoPayService.payCancel(paymentDto, paymentId);

            // 결제 상태 업데이트
            Payment payment = new Payment();
            payment.setId(paymentId);
            payment.setStatus("취소");
            payMapper.updateProductPayStatus(payment);

            log.info("상품 결제 취소 완료 - paymentId: {}", paymentId);

        } catch (Exception e) {
            log.error("상품 결제 취소 실패", e);
            throw new RuntimeException("결제 취소 중 오류가 발생했습니다.", e);
        }
    }
}
