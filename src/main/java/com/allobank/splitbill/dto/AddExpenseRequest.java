package com.allobank.splitbill.dto;

import com.allobank.splitbill.model.enums.ExpenseCategory;
import com.allobank.splitbill.model.enums.SplitStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidSplitSum
public class AddExpenseRequest {

    @NotNull
    private Long payerId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private SplitStrategy splitStrategy;

    @Valid
    @NotEmpty
    private List<SplitEntry> splits;

    private ExpenseCategory category;

    @Size(max = 500)
    private String description;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SplitEntry {

        @NotNull
        private Long participantId;

        private BigDecimal percentage;

        private BigDecimal amount;
    }
}