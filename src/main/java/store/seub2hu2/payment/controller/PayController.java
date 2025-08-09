package store.seub2hu2.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import store.seub2hu2.lesson.dto.*;
import store.seub2hu2.lesson.enums.ReservationStatus;
import store.seub2hu2.lesson.service.LessonFileService;
import store.seub2hu2.lesson.service.LessonService;
import store.seub2hu2.lesson.vo.LessonReservation;
import store.seub2hu2.mypage.dto.OrderResultDto;
import store.seub2hu2.mypage.dto.PaymentsDTO;
import store.seub2hu2.mypage.dto.ResponseDTO;
import store.seub2hu2.order.exception.PaymentAmountMismatchException;
import store.seub2hu2.order.mapper.OrderMapper;
import store.seub2hu2.order.service.OrderService;
import store.seub2hu2.order.vo.Order;
import store.seub2hu2.order.vo.OrderItem;
import store.seub2hu2.payment.dto.PaymentDto;
import store.seub2hu2.payment.dto.PaymentReadyResponse;
import store.seub2hu2.payment.mapper.PayMapper;
import store.seub2hu2.payment.service.KakaoPayService;
import store.seub2hu2.lesson.service.LessonReservationService;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.dto.CancelResponse;
import store.seub2hu2.payment.service.PaymentService;
import store.seub2hu2.payment.vo.Payment;
import store.seub2hu2.product.dto.ProdDetailDto;
import store.seub2hu2.security.user.LoginUser;
import store.seub2hu2.util.SessionUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pay")
public class PayController {

    private final KakaoPayService kakaoPayService;
    private final SessionUtils sessionUtils;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PayMapper payMapper;


    @PostMapping("/ready")
    public @ResponseBody PaymentReadyResponse payReady(@RequestBody PaymentDto paymentDto
    , @AuthenticationPrincipal LoginUser loginUser) {

        paymentDto.setUserNo(loginUser.getNo());
        // 카카오 결제 준비하기
        PaymentReadyResponse readyResponse = kakaoPayService.payReady(paymentDto);
        // 세션에 결제 고유번호(tid) 저장
        sessionUtils.addAttribute("tid", readyResponse.getTid());

        log.info("결제 고유번호: " + readyResponse.getTid());

        return readyResponse;
    }

    @GetMapping("/success")
    public String success(@RequestParam(name = "no", required = false, defaultValue = "0") int orderNo,
                          Model model) {
        try {
            if (orderNo <= 0) {
                log.error("잘못된 주문번호: {}", orderNo);
                return "redirect:/";
            }

            log.info("상품 결제 성공 - orderNo: {}", orderNo);

            // 상품 결제만 처리하므로 바로 주문 결과 조회
            OrderResultDto orderResultDto = orderService.getOrderResult(orderNo);
            if (orderResultDto == null) {
                log.error("주문 정보를 찾을 수 없음 - orderNo: {}", orderNo);
                return "redirect:/pay/error?code=ORDER_NOT_FOUND";
            }

            model.addAttribute("orderDetail", orderResultDto);
            return "mypage/order-pay-completed";

        } catch (Exception e) {
            log.error("결제 성공 페이지 처리 중 오류 - orderNo: {}", orderNo, e);
            return "redirect:/";
        }
    }


    @GetMapping("/completed")
    public String payCompleted(@RequestParam("pg_token") String pgToken,
                               @RequestParam("orderNo") int orderNo
                               , @AuthenticationPrincipal LoginUser loginUser) {

        String tid = sessionUtils.getAttribute("tid");

        try {
            // 1. 검증 + 결제 처리
            paymentService.validatePaymentCompletion(loginUser, tid, orderNo);
            paymentService.processPaymentComplete(tid, pgToken, orderNo, loginUser.getId());

            // 3. 세션 정리
            sessionUtils.clearPaymentSession();

            return "redirect:/pay/success?no=" + orderNo;

        } catch (IllegalArgumentException e) {
            log.error("검증 실패: {}", e.getMessage());

            // 검증 실패 유형에 따라 다른 에러 코드
            if (e.getMessage().contains("로그인")) {
                return "redirect:/login";
            } else if (e.getMessage().contains("세션")) {
                return "redirect:/pay/error?code=SESSION_EXPIRED";
            } else if (e.getMessage().contains("주문번호")) {
                return "redirect:/pay/error?code=INVALID_ORDER";
            }
            return "redirect:/pay/error";

        } catch (Exception e) {
            log.error("결제 처리 실패", e);
            sessionUtils.clearPaymentSession();
            return "redirect:/pay/error?code=PROCESSING_FAILED";
        }
    }

    @PostMapping("/cancel")
    public String cancelProductPayment(PaymentDto paymentDto,
                                       @AuthenticationPrincipal LoginUser loginUser,
                                       Model model) {
        try {
            // 기본 검증
            if (loginUser == null) {
                return "redirect:/login";
            }

            // 상품 결제만 처리
            if (!"상품".equals(paymentDto.getType())) {
                // 수정: 에러 JSP 페이지로
                return "redirect:/pay/error?code=UNSUPPORTED_TYPE";
            }

            log.info("상품 결제 취소 요청 - 사용자: {}", loginUser.getId());

            // PaymentService에 처리 위임
            paymentService.cancelProductPayment(paymentDto, loginUser.getId());

            return "redirect:/mypage/orderhistory?cancelled=true";

        } catch (Exception e) {
            log.error("상품 결제 취소 중 오류", e);
            // 수정: 에러 JSP 페이지로
            return "redirect:/pay/error?code=PROCESSING_FAILED";
        }
    }

    /**
     * 결제 실패 페이지 (카카오페이 결제 실패)
     */
    @GetMapping("/fail")
    public String payFail() {
        log.info("결제가 실패했습니다.");
        sessionUtils.clearPaymentSession(); // 세션 정리
        return "error/pay-fail";
    }

    /**
     * 결제 취소 페이지 (카카오페이에서 사용자가 취소)
     */
    @GetMapping("/cancel")
    public String payCancel() {
        log.info("사용자가 결제를 취소했습니다.");
        sessionUtils.clearPaymentSession(); // 세션 정리
        return "error/pay-cancel";
    }

    /**
     * 결제 에러 페이지
     */
    @GetMapping("/error")
    public String payError(@RequestParam(value = "code",required = false) String code, Model model) {
        log.info("결제 에러 - 코드: {}", code);

        String errorMessage = switch (code) {
            case "SESSION_EXPIRED" -> "결제 세션이 만료되었습니다. 다시 시도해주세요.";
            case "INVALID_ORDER" -> "잘못된 주문번호입니다.";
            case "PROCESSING_FAILED" -> "결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            case "UNSUPPORTED_TYPE" -> "지원하지 않는 결제 타입입니다.";
            case "USER_MISMATCH" -> "결제 사용자 정보가 일치하지 않습니다.";
            case "ORDER_NOT_FOUND" -> "주문 정보를 찾을 수 없습니다.";
            default -> "알 수 없는 오류가 발생했습니다.";
        };

        model.addAttribute("errorMessage", errorMessage);
        return "error/pay-error";
    }
}