package com.insidertracker.backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 1. /api/** 로 들어오는 모든 요청을
                .allowedOrigins("http://localhost:3000") // 2. http://localhost:3000 (React) 주소로부터의 요청을 허용한다
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS"); // 3. 허용할 HTTP 메소드
    }
}