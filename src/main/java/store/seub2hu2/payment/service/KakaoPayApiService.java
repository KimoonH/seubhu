package store.seub2hu2.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.dto.CancelResponse;
import store.seub2hu2.payment.dto.PaymentReadyResponse;

import java.util.Map;

@Slf4j
@Service
public class KakaoPayApiService {

    @Value("${kakaopay.secretKey}")
    private String secretKey;

    // 카카오페이 결제 준비 API 호출
    public PaymentReadyResponse requestPaymentReady(Map<String, String> parameters) {
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());
        RestTemplate template = new RestTemplate();
        String url = "https://open-api.kakaopay.com/online/v1/payment/ready";

        ResponseEntity<PaymentReadyResponse> responseEntity = template.postForEntity(url, requestEntity, PaymentReadyResponse.class);
        log.info("결제준비 응답객체: " + responseEntity.getBody());

        return responseEntity.getBody();
    }

    // 카카오페이 결제 승인 API 호출
    public ApproveResponse requestPaymentApprove(String tid, String pgToken) {
        Map<String, String> parameters = Map.of(
                "cid", "TC0ONETIME",
                "tid", tid,
                "partner_order_id", "1234567890",
                "partner_user_id", "seub2hu2",
                "pg_token", pgToken
        );

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());
        RestTemplate template = new RestTemplate();
        String url = "https://open-api.kakaopay.com/online/v1/payment/approve";

        ApproveResponse approveResponse = template.postForObject(url, requestEntity, ApproveResponse.class);
        log.info("결제승인 응답객체: " + approveResponse);

        return approveResponse;
    }

    // 카카오페이 결제 취소 API 호출
    public CancelResponse requestPaymentCancel(String tid, String cancelAmount, String quantity) {
        Map<String, String> parameters = Map.of(
                "cid", "TC0ONETIME",
                "tid", tid,
                "cancel_amount", cancelAmount,
                "cancel_tax_free_amount", "0",
                "quantity", quantity
        );

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());
        RestTemplate template = new RestTemplate();
        String url = "https://open-api.kakaopay.com/online/v1/payment/cancel";

        CancelResponse cancelResponse = template.postForObject(url, requestEntity, CancelResponse.class);
        log.info("결제취소 응답객체: " + cancelResponse);

        return cancelResponse;
    }

    // 카카오페이 API 헤더 생성
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        headers.set("Content-type", "application/json");
        return headers;
    }
}
