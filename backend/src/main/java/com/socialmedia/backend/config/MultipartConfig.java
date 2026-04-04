package com.socialmedia.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfig {
    // Spring Boot 3+ không còn MultipartConfigFactory, chỉ cần cấu hình qua application.properties
    // Nếu vẫn muốn cấu hình qua bean:
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement(
            null, 50L * 1024 * 1024, 60L * 1024 * 1024, 0
        );
    }
}
