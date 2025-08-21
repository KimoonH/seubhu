package store.seub2hu2.payment.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/api/test")
@Slf4j
public class AopTestController {

    private final Random random = new Random();

    /**
     * 🎲 랜덤하게 성공/실패하는 메서드 (AOP 테스트용)
     *
     * 🔧 수정사항:
     * - @RequestParam에 명시적으로 이름 지정: @RequestParam("failRate")
     * - 또는 value 속성 사용: @RequestParam(value = "failRate", defaultValue = "50")
     */
    @GetMapping("/retry")
    @Retryable(maxAttempts = 3)
    public String testRetryWithAop(@RequestParam(value = "failRate", defaultValue = "50") int failRate) {

        log.info("💳 테스트 메서드 실행 중... (실패확률: {}%)", failRate);

        // 랜덤하게 성공/실패 결정
        if (random.nextInt(100) < failRate) {
            log.error("💥 테스트 실패 발생!");
            throw new RuntimeException("테스트 실패 - 재시도 예정");
        }

        log.info("🎉 테스트 성공!");
        return "✅ 테스트 성공! AOP 모니터링이 잘 동작합니다!";
    }

    /**
     * 🔄 항상 실패하는 메서드 (최종 실패 테스트용)
     */
    @GetMapping("/retry-fail")
    @Retryable(maxAttempts = 3)
    public String testAlwaysFail() {
        log.info("💳 항상 실패하는 테스트 실행");
        throw new RuntimeException("이 메서드는 항상 실패합니다");
    }

    /**
     * 📝 AOP 없는 일반 메서드 (비교용)
     */
    @GetMapping("/normal")
    public String testNormalMethod() {
        log.info("🔹 일반 메서드 실행 (AOP 적용 안됨)");
        return "일반 메서드 실행 완료";
    }

    /**
     * 🎯 파라미터 없는 간단한 재시도 테스트
     * 가장 확실한 테스트 방법!
     */
    @GetMapping("/simple-retry")
    @Retryable(maxAttempts = 3)
    public String testSimpleRetry() {
        log.info("💳 간단한 재시도 테스트 실행");

        // 70% 확률로 실패
        if (random.nextInt(100) < 70) {
            log.error("💥 실패 발생!");
            throw new RuntimeException("70% 확률 실패");
        }

        log.info("🎉 성공!");
        return "✅ 간단한 재시도 테스트 성공!";
    }
}
