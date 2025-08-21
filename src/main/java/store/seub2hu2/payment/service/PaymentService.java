package store.seub2hu2.payment.service;

import io.netty.channel.ConnectTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import store.seub2hu2.common.service.SlackAlertService;
import store.seub2hu2.mypage.dto.OrderResultDto;
import store.seub2hu2.order.service.OrderService;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.payment.enums.PaymentMethod;
import store.seub2hu2.payment.enums.PaymentStatus;
import store.seub2hu2.payment.enums.PaymentType;
import store.seub2hu2.payment.exception.KakaoPayApproveException;
import store.seub2hu2.payment.exception.PaymentProcessingException;
import store.seub2hu2.payment.exception.PaymentValidationException;
import store.seub2hu2.payment.mapper.PayMapper;
import store.seub2hu2.payment.vo.Payment;
import store.seub2hu2.security.user.LoginUser;
import store.seub2hu2.util.SessionUtils;

import java.net.SocketTimeoutException;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final KakaoPayService kakaoPayService;
    private final OrderService orderService;
    private final PayMapper payMapper;
    private final SessionUtils sessionUtils;
    private final SlackAlertService slackAlertService;

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
        return Payment.builder()
                .id(tid)
                .userId(userId)
                .price(approveResponse.getAmount().getTotal())
                .amount(1)
                .method(PaymentMethod.KAKAO_PAY.getValue())
                .type(PaymentType.PRODUCT.getValue())
                .status(PaymentStatus.COMPLETED.getValue())
                .payCreatedDate(new Date())
                .build();
    }

    /**
     * 결제 승인부터 DB 저장까지 통합 처리
     */
    @Transactional
    public int processPaymentComplete(String tid, String pgToken, int orderNo, String userId) {
        log.info(" 결제 완료 처리 시작 - tid: {}, orderNo: {}, userId: {}", tid, orderNo, userId);

        try {
            // 1단계: 카카오페이 결제 승인
            ApproveResponse approveResponse = processPaymentApproval(tid, pgToken, orderNo);
            // 2단계: 결제 정보 DB 저장
            int payNo = savePaymentInfo(tid, userId, approveResponse);
            // 3단계: 주문과 결제 연결
            linkOrderWithPayment(orderNo, payNo);
            log.info(" 결제 완료 처리 성공 - orderNo: {}, payNo: {}, amount: {}",
                    orderNo, payNo, approveResponse.getAmount().getTotal());
            return payNo;
        } catch (Exception e) {
            //  결제 데이터 불일치 가능성 - 수동 확인 필요
            log.error(" 결제 완료 처리 실패 - 데이터 불일치 가능성");
            log.error(" 수동 확인 정보:");
            log.error("   - TID: {}", tid);
            log.error("   - 주문번호: {}", orderNo);
            log.error("   - 사용자ID: {}", userId);
            log.error("   - 오류: {}", e.getMessage());
            log.error(" 카카오페이에서 결제가 승인되었을 수 있으니 수동 확인 필요!");

            throw new PaymentProcessingException("결제 처리 중 오류가 발생했습니다. 고객센터로 문의해주세요.", e);
        }
    }

    /**
     * 1단계: 카카오페이 결제 승인 처리
     */
    @Retryable(
            include = {
                    SocketTimeoutException.class,
                    ConnectTimeoutException.class,
                    HttpServerErrorException.class,
                    ResourceAccessException.class,
                    RuntimeException.class,
                    KakaoPayApproveException.class,
                    Exception.class
            },
            exclude = {
                    PaymentValidationException.class,
                    IllegalArgumentException.class,
                    PaymentProcessingException.class
            },
            maxAttemptsExpression = "${payment.retry.approval.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${payment.retry.approval.initial-delay}",
                    multiplierExpression = "${payment.retry.approval.multiplier}",
                    maxDelayExpression = "${payment.retry.approval.max-delay}"
            )
    )
    public ApproveResponse processPaymentApproval(String tid, String pgToken, int orderNo) {

        log.info("결제 승인 요청 - tid: {}, orderNo: {}", tid, orderNo);

        try {
            ApproveResponse approveResponse = kakaoPayService.payApprove(tid, pgToken, orderNo);

            if (approveResponse == null || approveResponse.getAmount() == null) {
                throw new KakaoPayApproveException("결제 승인 응답이 올바르지 않습니다.");
            }

            log.info("결제 승인 완료 - tid: {}, amount: {}", tid, approveResponse.getAmount().getTotal());
            return approveResponse;

        } catch (PaymentValidationException | IllegalArgumentException e) {
            // 재시도하지 않을 예외는 그대로 던짐
            log.error(" 결제 승인 실패 (재시도 불가) - tid: {}, orderNo: {}", tid, orderNo, e);
            throw e;
        } catch (Exception e) {
            log.error(" 결제 승인 실패 (재시도 예정) - tid: {}, orderNo: {}", tid, orderNo, e);
            throw new KakaoPayApproveException("카카오페이 결제 승인에 실패했습니다.", e);
        }
    }

    /**
     * 2단계: 결제 정보 DB 저장
     */
    private int savePaymentInfo(String tid, String userId, ApproveResponse approveResponse) {
        log.info(" 결제 정보 저장 시작 - tid: {}, userId: {}", tid, userId);

        try {
            Payment payment = createPayment(tid, userId, approveResponse);
            payMapper.insertPay(payment);

            int payNo = payment.getNo();
            if (payNo <= 0) {
                throw new PaymentProcessingException("결제 정보 저장 후 ID 생성 실패");
            }

            log.info(" 결제 정보 저장 완료 - payNo: {}", payNo);
            return payNo;

        } catch (Exception e) {
            log.error(" 결제 정보 저장 실패 - tid: {}", tid, e);
            throw new PaymentProcessingException("결제 정보 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 3단계: 주문과 결제 연결
     */
    private void linkOrderWithPayment(int orderNo, int payNo) {
        log.info(" 주문-결제 연결 시작 - orderNo: {}, payNo: {}", orderNo, payNo);

        try {
            orderService.updateOrderPayNo(orderNo, payNo);
            log.info(" 주문-결제 연결 완료 - orderNo: {}, payNo: {}", orderNo, payNo);

        } catch (Exception e) {
            log.error(" 주문-결제 연결 실패 - orderNo: {}, payNo: {}", orderNo, payNo, e);
            throw new PaymentProcessingException("주문 정보 업데이트에 실패했습니다.", e);
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
            Payment payment = Payment.builder()
                    .id(paymentId)
                    .status(PaymentStatus.CANCELLED.getValue())
                    .payUpdatedDate(new Date())  // 보너스: 업데이트 시간도 추가
                    .build();

            payMapper.updateProductPayStatus(payment);

            log.info("상품 결제 취소 완료 - paymentId: {}", paymentId);

        } catch (Exception e) {
            log.error("상품 결제 취소 실패", e);
            throw new RuntimeException("결제 취소 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 결제 승인 재시도가 모두 실패했을 때 실행되는 복구 메서드
     */
    @Recover
    public ApproveResponse recoverPaymentApproval(Exception ex, String tid, String pgToken, int orderNo) {
        log.error("💥 결제 승인 최종 실패 - 모든 재시도 완료");
        log.error("📋 실패 정보:");
        log.error("   - TID: {}", tid);
        log.error("   - 주문번호: {}", orderNo);
        log.error("   - 최종 오류: {}", ex.getMessage());

        // 1단계: 임시 결제 상태로 저장 (나중에 수동 처리용)
        try {
            savePendingPayment(tid, orderNo, ex.getMessage());
            log.info("💾 임시 결제 상태 저장 완료 - 수동 처리 대기");
        } catch (Exception saveEx) {
            log.error("❌ 임시 결제 상태 저장 실패", saveEx);
        }

        // 2단계: 관리자 알림 발송 (실제 SlackAlertService 사용)
        try {
            sendPaymentFailureAlert(tid, orderNo, ex.getMessage());
            log.info("📢 관리자 알림 발송 완료");
        } catch (Exception alertEx) {
            log.error("❌ 관리자 알림 발송 실패", alertEx);
        }

        // 3단계: 사용자에게 친화적인 메시지와 함께 예외 발생
        throw new PaymentProcessingException(
                "결제 처리 중 일시적인 문제가 발생했습니다. " +
                        "결제 상태를 확인 중이니 잠시 후 마이페이지에서 확인해주세요. " +
                        "문제가 지속되면 고객센터(1588-1234)로 문의해주세요.",
                ex
        );
    }

    /**
     * 임시 결제 상태 저장 (수동 처리용)
     * 현재 시스템에 맞게 단순화
     */
    private void savePendingPayment(String tid, int orderNo, String errorMessage) {
        log.info("💾 임시 결제 상태 저장 - tid: {}, orderNo: {}", tid, orderNo);

        try {
            // 방법 1: 기존 Payment 객체로 FAILED 상태로 저장
            Payment failedPayment = Payment.builder()
                    .id(tid)
                    .userId("SYSTEM_RECOVER") // 복구 프로세스임을 표시
                    .status(PaymentStatus.FAILED.getValue()) // 기존 FAILED 상태 사용
                    .type(PaymentType.PRODUCT.getValue())
                    .method(PaymentMethod.KAKAO_PAY.getValue())
                    .price(0) // 금액은 나중에 조회해서 업데이트
                    .amount(1)
                    .payCreatedDate(new Date())
                    .build();

            // 기존 매퍼 메서드 사용
            payMapper.insertPay(failedPayment);
            log.info(" 실패 결제 정보 저장 완료 - 수동 확인 필요");

            // 방법 2: 로그 파일에 상세 정보 기록 (추가 안전장치)
            log.error(" [수동처리필요] TID:{}, OrderNo:{}, Error:{}", tid, orderNo, errorMessage);

        } catch (Exception e) {
            log.error(" 실패 결제 정보 저장 실패 - 완전 수동 처리 필요", e);
            log.error(" [긴급] TID:{}, OrderNo:{}, Error:{}", tid, orderNo, errorMessage);
            // 저장 실패해도 예외 발생시키지 않음 (알림은 보내야 하니까)
        }
    }

    /**
     * 관리자 알림 발송 - 실제 SlackAlertService 연동 완료!
     */
    private void sendPaymentFailureAlert(String tid, int orderNo, String errorMessage) {
        log.info("📢 관리자 알림 발송 - tid: {}", tid);

        try {
            // ✅ 실제 슬랙 알림 발송 (TODO 주석 제거!)
            slackAlertService.sendPaymentFailureAlert(tid, orderNo, errorMessage);

            // 기존 로그도 유지
            String alertMessage = String.format(
                    "[관리자 알림] 결제 승인 3회 연속 실패!\n\n" +
                            "📋 상세 정보:\n" +
                            "• TID: %s\n" +
                            "• 주문번호: %s\n" +
                            "• 발생시간: %s\n" +
                            "• 오류내용: %s\n\n" +
                            "⚠️ 즉시 확인이 필요합니다!",
                    tid, orderNo, new Date(), errorMessage
            );
            log.warn("🚨 {}", alertMessage);

        } catch (Exception e) {
            log.error("❌ 관리자 알림 발송 실패", e);
            // 알림 실패해도 예외 발생시키지 않음
        }
    }

    /**
     * 카카오페이 결제 취소 (재시도 로직 포함)
     */
    @Retryable(
            include = {
                    SocketTimeoutException.class,
                    ConnectTimeoutException.class,
                    HttpServerErrorException.class,
                    ResourceAccessException.class,
                    RuntimeException.class
            },
            exclude = {
                    PaymentValidationException.class,
                    IllegalArgumentException.class
            },
            maxAttemptsExpression = "${payment.retry.cancel.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${payment.retry.cancel.initial-delay}",
                    multiplierExpression = "${payment.retry.cancel.multiplier}",
                    maxDelayExpression = "${payment.retry.cancel.max-delay}"
            )
    )
    private void cancelPaymentWithRetry(PaymentDto paymentDto, String paymentId) {
        log.info("💳 결제 취소 요청 - paymentId: {}", paymentId);

        try {
            kakaoPayService.payCancel(paymentDto, paymentId);
            log.info("✅ 결제 취소 API 호출 성공 - paymentId: {}", paymentId);

        } catch (PaymentValidationException | IllegalArgumentException e) {
            log.error("❌ 결제 취소 실패 (재시도 불가) - paymentId: {}", paymentId, e);
            throw e;
        } catch (Exception e) {
            log.warn("⚠️ 결제 취소 실패 (재시도 예정) - paymentId: {}", paymentId, e);
            throw new RuntimeException("카카오페이 취소 API 호출 실패", e);
        }
    }

    /**
     * 결제 취소 재시도 실패 시 복구 메서드
     */
    @Recover
    private void recoverPaymentCancel(Exception ex, PaymentDto paymentDto, String paymentId) {  // ← Exception으로 변경
        log.error("🚨 결제 취소 최종 실패 - paymentId: {}", paymentId, ex);

        try {
            // ✅ 실제 슬랙 긴급 알림 발송 (TODO 주석 제거!)
            slackAlertService.sendPaymentCancelFailureAlert(
                    paymentId,
                    paymentDto.getTotalAmount(),
                    ex.getMessage()
            );

            // 기존 로그도 유지
            String alertMessage = String.format(
                    "🚨 긴급: 결제 취소 연속 실패!\n\n" +
                            "📋 상세 정보:\n" +
                            "• PaymentID: %s\n" +
                            "• 취소금액: %d원\n" +
                            "• 발생시간: %s\n" +
                            "• 오류내용: %s\n\n" +
                            "⚠️ 즉시 수동 처리 필요!",
                    paymentId, paymentDto.getTotalAmount(), new Date(), ex.getMessage()
            );
            log.error("🚨 [긴급알림] {}", alertMessage);

        } catch (Exception alertEx) {
            log.error("❌ 긴급 알림 발송 실패", alertEx);
        }

        throw new RuntimeException(
                "결제 취소 처리에 실패했습니다. 고객센터로 즉시 문의해주세요. (긴급 - 관리자 확인 중)",
                ex
        );
    }
}
