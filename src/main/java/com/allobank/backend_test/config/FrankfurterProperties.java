package com.allobank.backend_test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frankfurter")
public class FrankfurterProperties {

    private String baseUrl;

    private String baseCurrency;

    private String targetCurrency;

    private Historical historical = new Historical();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public Historical getHistorical() {
        return historical;
    }

    public void setHistorical(Historical historical) {
        this.historical = historical;
    }

    public static class Historical {
        private String dateRange = "2024-01-01..2024-01-05";

        public String getDateRange() {
            return dateRange;
        }

        public void setDateRange(String dateRange) {
            this.dateRange = dateRange;
        }
    }
}
