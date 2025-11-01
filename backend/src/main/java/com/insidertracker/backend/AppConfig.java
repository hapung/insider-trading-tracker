package com.insidertracker.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    // 1. sec-api.io 설정
    private final String SEC_API_BASE_URL = "https://api.sec-api.io";

    @Bean
    public WebClient secApiWebClient() {

        final int bufferSize = 10*1024*1024;

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(bufferSize)) // 3. 최대 메모리 내 버퍼 크기 증가
                .build();

        return WebClient.builder()
                .baseUrl(SEC_API_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies) // 4. 🔽🔽 [수정] 🔽🔽 WebClient에 이 전략을 적용
                .build();
    }

    // 2. 🔽🔽 [수정] 🔽🔽
    // Finnhub WebClient를 다시 추가합니다.
    private final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";

    @Bean
    public WebClient finnhubWebClient() {
        return WebClient.builder()
                .baseUrl(FINNHUB_BASE_URL)
                .build();
    }
}