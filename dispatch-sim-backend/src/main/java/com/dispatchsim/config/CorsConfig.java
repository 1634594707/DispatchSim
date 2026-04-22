package com.dispatchsim.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.getCors().getAllowedOrigins().toArray(String[]::new))
                .allowedMethods(properties.getCors().getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(properties.getCors().getAllowedHeaders().toArray(String[]::new))
                .allowCredentials(properties.getCors().isAllowCredentials());
    }
}
