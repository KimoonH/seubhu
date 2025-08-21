package store.seub2hu2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

@EnableCaching
@SpringBootApplication
@EnableRetry
public class Seub2hu2Application {

    public static void main(String[] args) {
        SpringApplication.run(Seub2hu2Application.class, args);
    }

}
