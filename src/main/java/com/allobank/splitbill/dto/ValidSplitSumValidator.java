package com.allobank.splitbill.dto;

import com.allobank.splitbill.model.enums.SplitStrategy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ValidSplitSumValidator implements ConstraintValidator<ValidSplitSum, AddExpenseRequest> {

    @Override
    public boolean isValid(AddExpenseRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getSplitStrategy() == null
                || request.getAmount() == null || request.getSplits() == null
                || request.getSplits().isEmpty()) {
            return true;
        }

        SplitStrategy strategy = request.getSplitStrategy();
        List<AddExpenseRequest.SplitEntry> splits = request.getSplits();
        BigDecimal total = request.getAmount();

        context.disableDefaultConstraintViolation();

        if (strategy == SplitStrategy.PERCENTAGE) {
            BigDecimal sum = BigDecimal.ZERO;
            for (AddExpenseRequest.SplitEntry s : splits) {
                if (s.getPercentage() == null) {
                    context.buildConstraintViolationWithTemplate("percentage is required for PERCENTAGE splits")
                            .addConstraintViolation();
                    return false;
                }
                sum = sum.add(s.getPercentage());
            }
            if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
                context.buildConstraintViolationWithTemplate("percentage splits must sum to 100, got " + sum)
                        .addConstraintViolation();
                return false;
            }
            return true;
        }

        if (strategy == SplitStrategy.EXACT) {
            BigDecimal sum = BigDecimal.ZERO;
            for (AddExpenseRequest.SplitEntry s : splits) {
                if (s.getAmount() == null) {
                    context.buildConstraintViolationWithTemplate("amount is required for EXACT splits")
                            .addConstraintViolation();
                    return false;
                }
                sum = sum.add(s.getAmount());
            }
            BigDecimal scaledSum = sum.setScale(total.scale(), RoundingMode.HALF_UP);
            if (scaledSum.compareTo(total) != 0) {
                context.buildConstraintViolationWithTemplate("exact splits must sum to " + total + ", got " + scaledSum)
                        .addConstraintViolation();
                return false;
            }
            return true;
        }

        return true;
    }
}