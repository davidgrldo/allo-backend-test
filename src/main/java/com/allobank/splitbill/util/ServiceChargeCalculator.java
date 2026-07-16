package com.allobank.splitbill.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ServiceChargeCalculator {

    private ServiceChargeCalculator() {}

    public static int computePct(String githubUsername) {
        String username = githubUsername == null ? "" : githubUsername.toLowerCase();
        int sum = 0;
        for (int i = 0; i < username.length(); i++) {
            sum += username.charAt(i);
        }
        return sum % 10;
    }

    public static BigDecimal computeAmount(BigDecimal total, int pct) {
        if (total == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal pctBd = BigDecimal.valueOf(pct);
        BigDecimal amount = total
                .multiply(pctBd)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return amount;
    }
}