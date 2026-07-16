package com.allobank.splitbill.dto;

import com.allobank.splitbill.model.enums.ExpenseCategory;
import com.allobank.splitbill.model.enums.SplitStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private Long id;
    private Long groupId;
    private Long payerId;
    private String payerName;
    private BigDecimal amount;
    private SplitStrategy splitStrategy;
    private ExpenseCategory category;
    private String description;
    private List<SplitResponse> splits;
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SplitResponse {
        private Long participantId;
        private String participantName;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}