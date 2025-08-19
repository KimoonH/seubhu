package store.seub2hu2.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    @Around("@annotation(performanceLog)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, PerformanceLog performanceLog) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String description = performanceLog.value().isEmpty() ? methodName : performanceLog.value();

        //  파라미터 정보 추가
        Object[] args = joinPoint.getArgs();

        //  시작 로그 (조건부)
        if (performanceLog.includeParams()) {
            log.info(" [{}] 시작 - 파라미터: {}", description, formatParameters(args));
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            //  임계값에 따른 로그 레벨 조정 (기존 단순 INFO에서 변경)
            logResult(description, executionTime, performanceLog, args, result);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            //  에러 로그 개선 (기존 단순 에러에서 상세 정보 추가)
            log.error(" [PERFORMANCE] {} 실행시간: {}ms (에러 발생: {})",
                    description, executionTime, e.getMessage());
            log.debug("예외 상세 정보", e);

            throw e;
        }
    }
    /**
     * 새로 추가된 메서드: 임계값에 따른 로그 레벨 결정
     */
    private void logResult(String description, long executionTime, PerformanceLog performanceLog,
                           Object[] args, Object result) {

        String paramInfo = performanceLog.includeParams() ?
                " | 파라미터: " + formatParameters(args) : "";
        String resultInfo = performanceLog.includeResult() ?
                " | 결과: " + formatResult(result) : "";

        if (executionTime >= performanceLog.errorThreshold()) {
            log.error("[PERFORMANCE] {} 실행시간: {}ms - 임계값 초과! (기준: {}ms){}{}",
                    description, executionTime, performanceLog.errorThreshold(), paramInfo, resultInfo);

        } else if (executionTime >= performanceLog.warnThreshold()) {
            log.warn("[PERFORMANCE] {} 실행시간: {}ms - 주의 필요 (기준: {}ms){}{}",
                    description, executionTime, performanceLog.warnThreshold(), paramInfo, resultInfo);

        } else {
            // 기존 형태와 유사하게 유지 (하위 호환성)
            log.info("[PERFORMANCE] {} 실행시간: {}ms{}{}",
                    description, executionTime, paramInfo, resultInfo);
        }
    }

    /**
     *  새로 추가된 메서드: 파라미터 포맷팅
     */
    private String formatParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "없음";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String || arg instanceof Number) {
                sb.append(arg);
            } else {
                // 복잡한 객체는 클래스명만 표시 (보안 고려)
                sb.append(arg.getClass().getSimpleName());
            }
        }
        return sb.toString();
    }

    /**
     *  결과값 포맷팅
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        if (result instanceof String || result instanceof Number || result instanceof Boolean) {
            return result.toString();
        }

        return result.getClass().getSimpleName();
    }

}
