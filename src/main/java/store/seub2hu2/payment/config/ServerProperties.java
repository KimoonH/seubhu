package store.seub2hu2.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 서버 관련 설정 정보
 */
@Data
@Component
@ConfigurationProperties(prefix = "server")
public class ServerProperties {

    private String ip;
}
