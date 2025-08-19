package store.seub2hu2.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceLog {

    /**
     * 성능 로그에 표시될 작업 설명
     * @return 작업 설명 (예: "상품 상세 조회")
     */
    String value();

    /**
     * 경고 임계값 (밀리초)
     * 이 시간을 초과하면 WARN 레벨로 로깅
     * @return 경고 임계값 (기본값: 1000ms = 1초)
     */
    long warnThreshold() default 1000;

    /**
     * 에러 임계값 (밀리초)
     * 이 시간을 초과하면 ERROR 레벨로 로깅
     * @return 에러 임계값 (기본값: 3000ms = 3초)
     */
    long errorThreshold() default 3000;

    /**
     * 파라미터를 로그에 포함할지 여부
     * @return true이면 메서드 파라미터를 로그에 출력 (기본값: true)
     */
    boolean includeParams() default true;

    /**
     * 결과값을 로그에 포함할지 여부
     * @return true이면 메서드 반환값을 로그에 출력 (기본값: false)
     */
    boolean includeResult() default false;

    /**
     * 성능 메트릭을 Prometheus 등 외부 시스템에 전송할지 여부
     * @return true이면 메트릭 수집 (기본값: true)
     */
    boolean enableMetrics() default true;
}
