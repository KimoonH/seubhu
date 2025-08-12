package store.seub2hu2.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import store.seub2hu2.payment.config.KakaoPayProperties;
import store.seub2hu2.payment.dto.ApproveResponse;
import store.seub2hu2.payment.dto.CancelResponse;
import store.seub2hu2.payment.dto.PaymentReadyResponse;
import store.seub2hu2.payment.exception.KakaoPayApproveException;
import store.seub2hu2.payment.exception.KakaoPayCancelException;
import store.seub2hu2.payment.exception.KakaoPayReadyException;
import store.seub2hu2.payment.exception.PaymentNetworkException;

import java.net.SocketTimeoutException;
import java.util.Map;

import static store.seub2hu2.payment.constant.PaymentConstants.KakaoPay.*;
import static store.seub2hu2.payment.constant.PaymentConstants.TAX_FREE_AMOUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayApiService {

    private final KakaoPayProperties kakaoPayProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    // 카카오페이 결제 준비 API 호출
    public PaymentReadyResponse requestPaymentReady(Map<String, String> parameters) {
        try {
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());

            ResponseEntity<PaymentReadyResponse> responseEntity = restTemplate.postForEntity(
                    kakaoPayProperties.getReadyUrl()
                    , requestEntity
                    , PaymentReadyResponse.class);
            log.info("결제준비 응답객체: {}", responseEntity.getBody());

            return responseEntity.getBody();

        } catch (HttpClientErrorException e) {
            log.error("카카오페이 결제준비 클라이언트 오류 - 상태코드: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new KakaoPayReadyException("결제 정보가 올바르지 않습니다. 다시 확인해주세요.", e);

        } catch (HttpServerErrorException e) {
            log.error("카카오페이 결제준비 서버 오류 - 상태코드: {}", e.getStatusCode());
            throw new KakaoPayReadyException("카카오페이 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.", e);

        } catch (ResourceAccessException e) {
            log.error("카카오페이 결제준비 네트워크 오류: {}", e.getMessage());
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new PaymentNetworkException("네트워크 연결이 불안정합니다. 잠시 후 다시 시도해주세요.", e);
            }
            throw new PaymentNetworkException("네트워크 연결에 문제가 발생했습니다. 인터넷 연결을 확인해주세요.", e);

        } catch (RestClientException e) {
            log.error("카카오페이 결제준비 API 호출 실패: {}", e.getMessage(), e);
            throw new KakaoPayReadyException("결제 준비 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);

        } catch (Exception e) {
            log.error("결제준비 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new KakaoPayReadyException("결제 준비 중 오류가 발생했습니다.", e);
        }
    }

    // 카카오페이 결제 승인 API 호출
    public ApproveResponse requestPaymentApprove(String tid, String pgToken, String partnerOrderId) {
        try {
            Map<String, String> parameters = Map.of(
                    PARAM_CID, kakaoPayProperties.getCid(),
                    PARAM_TID, tid,
                    PARAM_PARTNER_ORDER_ID, partnerOrderId,
                    PARAM_PARTNER_USER_ID, kakaoPayProperties.getPartnerUserId(),
                    PARAM_PG_TOKEN, pgToken
            );

            log.info("=== 결제승인 partner_order_id: {}", parameters.get(PARAM_PARTNER_ORDER_ID));

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());

            ApproveResponse approveResponse = restTemplate.postForObject(
                    kakaoPayProperties.getApproveUrl()
                    , requestEntity
                    , ApproveResponse.class);
            log.info("결제승인 응답객체: {}", approveResponse);

            return approveResponse;

        } catch (HttpClientErrorException e) {
            log.error("카카오페이 결제승인 클라이언트 오류 - 상태코드: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 400) {
                throw new KakaoPayApproveException("결제 승인 정보가 올바르지 않습니다. 다시 시도해주세요.", e);
            }
            throw new KakaoPayApproveException("결제 승인에 실패했습니다. 고객센터로 문의해주세요.", e);

        } catch (HttpServerErrorException e) {
            log.error("카카오페이 결제승인 서버 오류 - 상태코드: {}", e.getStatusCode());
            throw new KakaoPayApproveException("카카오페이 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.", e);

        } catch (ResourceAccessException e) {
            log.error("카카오페이 결제승인 네트워크 오류: {}", e.getMessage());
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new PaymentNetworkException("네트워크 연결이 불안정합니다. 결제 상태를 확인해주세요.", e);
            }
            throw new PaymentNetworkException("네트워크 연결에 문제가 발생했습니다. 결제 상태를 확인해주세요.", e);

        } catch (RestClientException e) {
            log.error("카카오페이 결제승인 API 호출 실패: {}", e.getMessage(), e);
            throw new KakaoPayApproveException("결제 승인 중 오류가 발생했습니다. 고객센터로 문의해주세요.", e);

        } catch (Exception e) {
            log.error("결제승인 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new KakaoPayApproveException("결제 승인 중 오류가 발생했습니다.", e);
        }
    }

    // 카카오페이 결제 취소 API 호출
    public CancelResponse requestPaymentCancel(String tid, String cancelAmount, String quantity) {
        try {
            Map<String, String> parameters = Map.of(
                    PARAM_CID, kakaoPayProperties.getCid(),
                    PARAM_TID, tid,
                    PARAM_CANCEL_AMOUNT, cancelAmount,
                    PARAM_CANCEL_TAX_FREE_AMOUNT, TAX_FREE_AMOUNT,
                    PARAM_QUANTITY, quantity
            );

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parameters, getHeaders());

            CancelResponse cancelResponse = restTemplate.postForObject(
                    kakaoPayProperties.getCancelUrl()
                    , requestEntity
                    , CancelResponse.class);
            log.info("결제취소 응답객체: {}", cancelResponse);

            return cancelResponse;

        } catch (HttpClientErrorException e) {
            log.error("카카오페이 결제취소 클라이언트 오류 - 상태코드: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 400) {
                throw new KakaoPayCancelException("취소할 수 없는 결제입니다. 결제 상태를 확인해주세요.", e);
            }
            throw new KakaoPayCancelException("결제 취소에 실패했습니다. 고객센터로 문의해주세요.", e);

        } catch (HttpServerErrorException e) {
            log.error("카카오페이 결제취소 서버 오류 - 상태코드: {}", e.getStatusCode());
            throw new KakaoPayCancelException("카카오페이 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.", e);

        } catch (ResourceAccessException e) {
            log.error("카카오페이 결제취소 네트워크 오류: {}", e.getMessage());
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new PaymentNetworkException("네트워크 연결이 불안정합니다. 취소 상태를 확인해주세요.", e);
            }
            throw new PaymentNetworkException("네트워크 연결에 문제가 발생했습니다. 취소 상태를 확인해주세요.", e);

        } catch (RestClientException e) {
            log.error("카카오페이 결제취소 API 호출 실패: {}", e.getMessage(), e);
            throw new KakaoPayCancelException("결제 취소 중 오류가 발생했습니다. 고객센터로 문의해주세요.", e);

        } catch (Exception e) {
            log.error("결제취소 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new KakaoPayCancelException("결제 취소 중 오류가 발생했습니다.", e);
        }
    }

    // 카카오페이 API 헤더 생성
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + kakaoPayProperties.getSecretKey());
        headers.set(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);
        return headers;
    }
}
