package com.insidertracker.backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 🔽🔽 [수정] 🔽🔽
                .allowedOrigins("https://insider-trading-tracker.vercel.app") // Vercel 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}