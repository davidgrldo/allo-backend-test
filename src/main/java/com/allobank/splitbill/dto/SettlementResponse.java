package com.allobank.splitbill.dto;

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
public class SettlementResponse {

    private Long groupId;
    private BigDecimal totalExpenses;
    private int serviceChargePct;
    private BigDecimal serviceChargeAmount;
    private List<BalanceEntry> balances;
    private List<TransactionEntry> transactions;
    private List<CategorySummary> categorySummaries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceEntry {
        private Long participantId;
        private String name;
        private BigDecimal netBalance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionEntry {
        private Long fromParticipantId;
        private String fromName;
        private Long toParticipantId;
        private String toName;
        private BigDecimal amount;
    }
}