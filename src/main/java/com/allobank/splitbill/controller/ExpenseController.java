package com.allobank.splitbill.controller;

import com.allobank.splitbill.dto.AddExpenseRequest;
import com.allobank.splitbill.dto.ExpenseResponse;
import com.allobank.splitbill.model.Expense;
import com.allobank.splitbill.model.ExpenseSplit;
import com.allobank.splitbill.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse add(@PathVariable Long groupId, @Valid @RequestBody AddExpenseRequest request) {
        List<ExpenseService.SplitInput> splits = request.getSplits().stream()
                .map(s -> new ExpenseService.SplitInput(s.getParticipantId(), s.getPercentage(), s.getAmount()))
                .toList();
        Expense expense = expenseService.addExpense(groupId, request.getPayerId(), request.getAmount(),
                request.getSplitStrategy(), splits, request.getCategory(), request.getDescription());
        return toResponse(expense);
    }

    @GetMapping
    public List<ExpenseResponse> list(@PathVariable Long groupId) {
        return expenseService.listByGroup(groupId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ExpenseResponse toResponse(Expense expense) {
        List<ExpenseResponse.SplitResponse> splits = expense.getSplits().stream()
                .map(s -> ExpenseResponse.SplitResponse.builder()
                        .participantId(s.getParticipant().getId())
                        .participantName(s.getParticipant().getName())
                        .amount(s.getAmount())
                        .percentage(s.getPercentage())
                        .build())
                .toList();
        return ExpenseResponse.builder()
                .id(expense.getId())
                .groupId(expense.getGroup().getId())
                .payerId(expense.getPayer().getId())
                .payerName(expense.getPayer().getName())
                .amount(expense.getAmount())
                .splitStrategy(expense.getSplitStrategy())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .splits(splits)
                .createdAt(expense.getCreatedAt())
                .build();
    }
}