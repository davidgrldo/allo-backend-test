package com.allobank.backend_test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplateFactoryBean restTemplateFactoryBean(FrankfurterProperties props) {
        return new RestTemplateFactoryBean(props.getBaseUrl());
    }
}
