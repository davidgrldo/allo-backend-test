package com.allobank.backend_test.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Duration;

public class RestTemplateFactoryBean implements FactoryBean<RestTemplate> {

    private final String baseUrl;

    public RestTemplateFactoryBean(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public RestTemplate getObject() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(baseUrl));
        return restTemplate;
    }

    @Override
    public Class<?> getObjectType() {
        return RestTemplate.class;
    }
}
