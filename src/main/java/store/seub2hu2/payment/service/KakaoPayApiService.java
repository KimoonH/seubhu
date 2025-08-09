package store.seub2hu2.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
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

    @Value("${kakaopay.cid}")
    private String cid;

    @Value("${kakaopay.partner-user-id}")
    private String partnerUserId;

    // 3단계: API URL들 외부화
    @Value("${kakaopay.api.ready-url}")
    private String readyUrl;

    @Value("${kakaopay.api.approve-url}")
    private String approveUrl;

    @Value("${kakaopay.api.cancel-url}")
    private String cancelUrl;

    // 2단계: RestTemplate 재사용
    private final RestTemplate restTemplate = new RestTemplate();

    // 카카오페이 결제 준비 API 호출
    public PaymentReadyResponse requestPaymentReady(Map<String, String> parameters) {
        try {
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());

            ResponseEntity<PaymentReadyResponse> responseEntity = restTemplate.postForEntity(readyUrl, requestEntity, PaymentReadyResponse.class);
            log.info("결제준비 응답객체: {}", responseEntity.getBody());

            return responseEntity.getBody();
        } catch (RestClientException e) {
            log.error("카카오페이 결제준비 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("결제 준비 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (Exception e) {
            log.error("결제준비 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("결제 준비 중 오류가 발생했습니다.", e);
        }
    }

    // 카카오페이 결제 승인 API 호출
    public ApproveResponse requestPaymentApprove(String tid, String pgToken, String partnerOrderId) {
        try {
            Map<String, String> parameters = Map.of(
                    "cid", cid,
                    "tid", tid,
                    "partner_order_id", partnerOrderId,
                    "partner_user_id", partnerUserId,
                    "pg_token", pgToken
            );

            log.info("=== 결제승인 partner_order_id: {}", parameters.get("partner_order_id"));

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());

            ApproveResponse approveResponse = restTemplate.postForObject(approveUrl, requestEntity, ApproveResponse.class);
            log.info("결제승인 응답객체: {}", approveResponse);

            return approveResponse;
        } catch (RestClientException e) {
            log.error("카카오페이 결제승인 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("결제 승인 중 오류가 발생했습니다. 고객센터로 문의해주세요.", e);
        } catch (Exception e) {
            log.error("결제승인 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("결제 승인 중 오류가 발생했습니다.", e);
        }
    }

    // 카카오페이 결제 취소 API 호출
    public CancelResponse requestPaymentCancel(String tid, String cancelAmount, String quantity) {
        try {
            Map<String, String> parameters = Map.of(
                    "cid", cid,
                    "tid", tid,
                    "cancel_amount", cancelAmount,
                    "cancel_tax_free_amount", "0",
                    "quantity", quantity
            );

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());

            CancelResponse cancelResponse = restTemplate.postForObject(cancelUrl, requestEntity, CancelResponse.class);
            log.info("결제취소 응답객체: {}", cancelResponse);

            return cancelResponse;
        } catch (RestClientException e) {
            log.error("카카오페이 결제취소 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("결제 취소 중 오류가 발생했습니다. 고객센터로 문의해주세요.", e);
        } catch (Exception e) {
            log.error("결제취소 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("결제 취소 중 오류가 발생했습니다.", e);
        }
    }

    // 카카오페이 API 헤더 생성
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        headers.set("Content-type", "application/json");
        return headers;
    }
}
