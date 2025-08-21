package store.seub2hu2.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import store.seub2hu2.common.service.SlackAlertService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RetryMonitoringAspect {

    private final SlackAlertService slackAlertService;

    //기존 모니터링 데이터
    private final ConcurrentHashMap<String, AtomicInteger> attemptCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> totalRetryStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> consecutiveFailures = new ConcurrentHashMap<>();

    //설정값
    @Value("${slack.alerts.consecutive-failure-threshold:3}")
    private int consecutiveFailureThreshold;

    /**
     *  @Retryable이 붙은 모든 메서드를 감시 (슬랙 알림 추가)
     */
    @Around("@annotation(org.springframework.retry.annotation.Retryable)")
    public Object monitorRetryableMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        // 📋 메서드 정보 추출
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        //현재 시도 횟수 증가
        AtomicInteger currentAttempt = attemptCounters.computeIfAbsent(fullMethodName,
                k -> new AtomicInteger(0));
        int attemptNumber = currentAttempt.incrementAndGet();

        //시도 횟수에 따른 로그 레벨 조정
        if (attemptNumber == 1) {
            log.info("메서드 실행 시작: {} (1번째 시도)", fullMethodName);
        } else {
            log.warn("재시도 실행: {} ({}번째 시도)", fullMethodName, attemptNumber);

            // 📊 재시도 통계 업데이트 (2번째 시도부터 카운트)
            totalRetryStats.computeIfAbsent(fullMethodName, k -> new AtomicInteger(0))
                    .incrementAndGet();
        }

        try {
            // ⚡ 실제 메서드 실행
            Object result = joinPoint.proceed();

            //성공 시 처리
            handleMethodSuccess(fullMethodName, attemptNumber, currentAttempt);

            return result;

        } catch (Exception e) {
            //실패 시 처리 (슬랙 알림 포함)
            handleMethodFailure(fullMethodName, attemptNumber, e);

            // 예외를 다시 던져서 Spring Retry가 동작하도록
            throw e;
        }
    }

    /**
     *메서드 성공 시 처리 로직
     */
    private void handleMethodSuccess(String fullMethodName, int attemptNumber, AtomicInteger currentAttempt) {
        if (attemptNumber == 1) {
            log.info("메서드 성공: {} (1번만에 성공)", fullMethodName);
        } else {
            log.info("재시도 후 성공: {} (총 {}번 시도 후 성공)", fullMethodName, attemptNumber);

            // 중요: 3번 이상 재시도 후 성공하면 슬랙 알림
            if (attemptNumber >= 3) {
                log.info("재시도 후 성공 - 슬랙 알림 발송");

                slackAlertService.sendInfoAlert(
                        "재시도 후 성공",
                        String.format(
                                "메서드 `%s`가 %d번 시도 후 성공했습니다.\n" +
                                        "• 최종 성공까지 시간이 걸렸으니 시스템 상태를 확인해보세요.",
                                fullMethodName, attemptNumber
                        )
                );
            }
        }

        // 성공 시 카운터 리셋
        currentAttempt.set(0);
        consecutiveFailures.remove(fullMethodName);

        // 통계 출력 (재시도가 있었던 경우만)
        if (attemptNumber > 1) {
            logCurrentStats(fullMethodName);
        }
    }

    /**
     * 메서드 실패 시 처리 로직 (슬랙 알림 포함)
     */
    private void handleMethodFailure(String fullMethodName, int attemptNumber, Exception e) {
        if (attemptNumber == 1) {
            log.warn("메서드 실패: {}, 오류: {} (재시도 예정)", fullMethodName, e.getMessage());
        } else {
            log.error("재시도 실패: {} ({}번째 시도), 오류: {}",
                    fullMethodName, attemptNumber, e.getMessage());
        }

        //연속 실패 체크 및 슬랙 알림
        int consecutiveCount = consecutiveFailures.computeIfAbsent(fullMethodName, k -> new AtomicInteger(0))
                .incrementAndGet();

        if (consecutiveCount >= consecutiveFailureThreshold) {
            log.warn("연속 실패 임계값 초과: {} ({}번 연속)", fullMethodName, consecutiveCount);

            // 📱 슬랙 연속 실패 알림 발송
            slackAlertService.sendConsecutiveFailureAlert(
                    fullMethodName,
                    consecutiveCount,
                    e.getMessage()
            );
        }
    }

    /**
     *현재 재시도 통계 출력 (기존 코드 유지)
     */
    private void logCurrentStats(String methodName) {
        int retryCount = totalRetryStats.getOrDefault(methodName, new AtomicInteger(0)).get();
        log.info("[통계] {} - 총 재시도 횟수: {}번", methodName, retryCount);
    }

    /**
     *전체 재시도 통계 조회 (기존 코드 유지)
     */
    public void printAllStats() {
        log.info("===== 전체 재시도 통계 =====");

        if (totalRetryStats.isEmpty()) {
            log.info("재시도 발생한 메서드가 없습니다");
            return;
        }

        totalRetryStats.forEach((methodName, retryCount) -> {
            log.info(" {} - 총 재시도: {}번", methodName, retryCount.get());
        });

        log.info("===========================");
    }

    /**
     * 통계 초기화
     */
    public void resetStats() {
        attemptCounters.clear();
        totalRetryStats.clear();
        consecutiveFailures.clear();
        log.info("재시도 통계가 초기화되었습니다");
    }
}
