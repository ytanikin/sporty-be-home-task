package com.sporty.behometask.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate with appropriate timeouts.
 * Timeouts are critical for preventing thread exhaustion in production.
 */
@Slf4j
@Configuration
public class RestTemplateConfig {
    
    @Value("${aviation.api.connection-timeout:5000}")
    private int connectionTimeout;
    
    @Value("${aviation.api.read-timeout:10000}")
    private int readTimeout;
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        log.info("Configuring RestTemplate with connection timeout: {}ms, read timeout: {}ms", connectionTimeout, readTimeout);
        
        return builder.setConnectTimeout(Duration.ofMillis(connectionTimeout)).setReadTimeout(Duration.ofMillis(readTimeout))
                .requestFactory(this::createRequestFactory).build();
    }
    
    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        return new BufferingClientHttpRequestFactory(factory);
    }
}


