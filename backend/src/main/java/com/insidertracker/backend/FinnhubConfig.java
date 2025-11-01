package com.insidertracker.backend;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// @Getter와 @Setter 어노테이션을 완전히 삭제했습니다.
@Configuration
@ConfigurationProperties(prefix = "finnhub")
public class FinnhubConfig {

    private String apiKey;

    // Getter를 우리가 직접 작성합니다.
    public String getApiKey() {
        return apiKey;
    }

    // Setter를 우리가 직접 작성합니다. (가장 중요)
    // (스프링이 이 메소드를 호출해서 application.properties의 값을 주입합니다.)
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}