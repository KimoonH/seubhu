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

    private final LessonReservationService lessonReservationService;
    private final LessonService lessonService;
    private final LessonFileService lessonFileService;
    private final PaymentService paymentService;

    private final OrderService orderService;
    private final PayMapper payMapper;
    private final OrderMapper orderMapper;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/form")
    public String payment(LessonDto lessonDto,
                          Model model) {
        log.info("/form lessonDto = {}", lessonDto);

        Map<String, String> images = lessonFileService.getImagesByLessonNo(lessonDto.getLessonNo());

        model.addAttribute("lessonDto", lessonDto);
        model.addAttribute("images", images);
        return "lesson/lesson-payment-form";
    }

    @PostMapping("/ready")
    public @ResponseBody ReadyResponse payReady(@RequestBody PaymentDto paymentDto
    , @AuthenticationPrincipal LoginUser loginUser) {

        paymentDto.setUserNo(loginUser.getNo());
        // 카카오 결제 준비하기
        ReadyResponse readyResponse = kakaoPayService.payReady(paymentDto);
        // 세션에 결제 고유번호(tid) 저장
        sessionUtils.addAttribute("tid", readyResponse.getTid());

        log.info("결제 고유번호: " + readyResponse.getTid());

        return readyResponse;
    }

    @GetMapping("/completed")
    public String payCompleted(@RequestParam("pg_token") String pgToken
                               , @AuthenticationPrincipal LoginUser loginUser
            , @RequestParam  Map<String, Object> param
            , Model model) {

        String tid = sessionUtils.getAttribute("tid");
        log.info("결제승인 요청을 인증하는 토큰: " + pgToken);
        log.info("결제 고유번호: " + tid);

        String type = (String) param.get("type");

        if (type.equals("상품")) {
            // 결재정보를 저장한다.
            String orderStr = (String) param.get("orderNo");
            int orderNo = Integer.parseInt(orderStr);
            String userId = loginUser.getId();


            // 카카오 결제 요청하기
            ApproveResponse approveResponse = kakaoPayService.payApprove(tid, pgToken, orderNo);

            PaymentDto paymentDto = new PaymentDto();
            paymentDto.setUserId(userId);
            paymentDto.setPaymentId(tid);

            Payment payment = new Payment();
            payment.setId(paymentDto.getPaymentId());
            payment.setUserId(paymentDto.getUserId());
            payment.setStatus("결제완료");
            payment.setType("상품");
            payment.setMethod("카카오페이");
            payment.setAmount(1);
            payment.setPrice(approveResponse.getAmount().getTotal());
            payMapper.insertPay(payment);

            int payNo = payment.getNo();

            // order pay 번호 update
            orderService.updateOrderPayNo(orderNo, payNo);
            
            // 장바구니에서 삭제


            return "redirect:/pay/success?no="+ orderNo;

        }


        return "redirect:/pay/success";
    }

    // 결제 취소 요청
    @PostMapping("/cancel")
    public String payCancel(PaymentDto paymentDto
                           , @AuthenticationPrincipal LoginUser loginUser
            , Model model) {


        if("상품".equals(paymentDto.getType())) {

            // 주문관련 정보 수정
            OrderResultDto dto = orderService.cancelOrder(paymentDto);

            String paymentId = dto.getPayId();
            paymentDto.setTotalAmount(dto.getPayPrice());

            // 카카오 결제 취소
            CancelResponse cancelResponse = kakaoPayService.payCancel(paymentDto, paymentId);

            // 결재정보를 변경
            Payment p2 = new Payment();
            p2.setId(paymentId);
            p2.setStatus("취소");
            payMapper.updateProductPayStatus(p2);


            model.addAttribute("cancelResponse", cancelResponse);

            return "redirect:/mypage/orderhistory";
        }

        return null;
    }

    // 결제 성공 화면
    @GetMapping("/success")
    public String success(@RequestParam(name ="no", required = false, defaultValue = "0") int orderNo, Model model) {
        String tid= sessionUtils.getAttribute("tid");
        String type = paymentService.getPaymentTypeById(tid);
        System.out.println("------------------------- tid: " + tid);
        System.out.println("------------------------- 타입: " + type);

        if (type.equals("레슨")) {
            LessonReservation lessonReservation = lessonReservationService.getLessonReservationByPayId(tid);
            int lessonNo = lessonReservation.getLesson().getLessonNo();
            Map<String, String> startAndEnd = lessonService.getStartAndEnd(lessonNo);
            log.info("/success lessonReservation 객체 = {} ", lessonReservation);
            Map<String, String> images = lessonFileService.getImagesByLessonNo(lessonNo);
            model.addAttribute("lessonReservation", lessonReservation);
            model.addAttribute("images", images);
            model.addAttribute("startDate", startAndEnd.get("startDate"));
            model.addAttribute("startTime", startAndEnd.get("startTime"));
            model.addAttribute("endTime", startAndEnd.get("endTime"));
        }

        if (type.equals("상품")) {
            OrderResultDto orderResultDto = orderService.getOrderResult(orderNo);

            model.addAttribute("orderDetail", orderResultDto);

            return "mypage/order-pay-completed";
        }

        return "lesson/lesson-pay-completed";
    }
}