package com.allobank.splitbill.controller;

import com.allobank.splitbill.dto.SettlementResponse;
import com.allobank.splitbill.service.SettlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public SettlementResponse get(@PathVariable Long groupId) {
        SettlementService.SettlementResult result = settlementService.computeSettlement(groupId);

        List<SettlementResponse.BalanceEntry> balances = result.balances().stream()
                .map(b -> SettlementResponse.BalanceEntry.builder()
                        .participantId(b.participantId())
                        .name(b.name())
                        .netBalance(b.netBalance())
                        .build())
                .toList();

        List<SettlementResponse.TransactionEntry> transactions = result.transactions().stream()
                .map(t -> SettlementResponse.TransactionEntry.builder()
                        .fromParticipantId(t.fromParticipantId())
                        .fromName(t.fromName())
                        .toParticipantId(t.toParticipantId())
                        .toName(t.toName())
                        .amount(t.amount())
                        .build())
                .toList();

        List<com.allobank.splitbill.dto.CategorySummary> summaries = result.categorySummaries().stream()
                .map(c -> com.allobank.splitbill.dto.CategorySummary.builder()
                        .category(c.category())
                        .totalAmount(c.totalAmount())
                        .expenseCount(c.expenseCount())
                        .build())
                .toList();

        return SettlementResponse.builder()
                .groupId(result.groupId())
                .totalExpenses(result.totalExpenses())
                .serviceChargePct(result.serviceChargePct())
                .serviceChargeAmount(result.serviceChargeAmount())
                .balances(balances)
                .transactions(transactions)
                .categorySummaries(summaries)
                .build();
    }
}