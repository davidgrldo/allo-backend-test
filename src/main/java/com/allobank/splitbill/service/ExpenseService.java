package com.allobank.splitbill.service;

import com.allobank.splitbill.model.Expense;
import com.allobank.splitbill.model.ExpenseSplit;
import com.allobank.splitbill.model.Group;
import com.allobank.splitbill.model.Participant;
import com.allobank.splitbill.model.enums.SplitStrategy;
import com.allobank.splitbill.repository.ExpenseRepository;
import com.allobank.splitbill.repository.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;

    public ExpenseService(ExpenseRepository expenseRepository, GroupRepository groupRepository) {
        this.expenseRepository = expenseRepository;
        this.groupRepository = groupRepository;
    }

    public Expense addExpense(Long groupId, Long payerId, BigDecimal amount,
                              SplitStrategy strategy, List<SplitInput> splitInputs,
                              com.allobank.splitbill.model.enums.ExpenseCategory category,
                              String description) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));

        Map<Long, Participant> groupMembers = new HashMap<>();
        for (Participant p : group.getParticipants()) {
            groupMembers.put(p.getId(), p);
        }

        Participant payer = groupMembers.get(payerId);
        if (payer == null) {
            throw new IllegalArgumentException("Payer " + payerId + " is not a member of group " + groupId);
        }

        Map<Long, Participant> splitParticipants = new HashMap<>();
        for (SplitInput input : splitInputs) {
            Participant p = groupMembers.get(input.participantId());
            if (p == null) {
                throw new IllegalArgumentException("Participant " + input.participantId()
                        + " is not a member of group " + groupId);
            }
            splitParticipants.put(input.participantId(), p);
        }

        List<ExpenseSplit> splits = computeSplits(amount, strategy, splitInputs, splitParticipants);

        Expense expense = Expense.builder()
                .group(group)
                .payer(payer)
                .amount(amount)
                .splitStrategy(strategy)
                .category(category)
                .description(description)
                .splits(splits)
                .build();
        for (ExpenseSplit split : splits) {
            split.setExpense(expense);
        }
        return expenseRepository.save(expense);
    }

    public List<Expense> listByGroup(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new EntityNotFoundException("Group not found: " + groupId);
        }
        return expenseRepository.findByGroupId(groupId);
    }

    private List<ExpenseSplit> computeSplits(BigDecimal total, SplitStrategy strategy,
                                            List<SplitInput> inputs, Map<Long, Participant> participants) {
        if (strategy == SplitStrategy.EQUAL) {
            BigDecimal baseShare = total.divide(BigDecimal.valueOf(inputs.size()), 4, RoundingMode.HALF_UP);
            BigDecimal accumulated = BigDecimal.ZERO;
            List<ExpenseSplit> splits = new java.util.ArrayList<>();
            for (int i = 0; i < inputs.size(); i++) {
                SplitInput input = inputs.get(i);
                BigDecimal share;
                if (i == inputs.size() - 1) {
                    share = total.subtract(accumulated).setScale(4, RoundingMode.HALF_UP);
                } else {
                    share = baseShare;
                    accumulated = accumulated.add(share);
                }
                splits.add(ExpenseSplit.builder()
                        .participant(participants.get(input.participantId()))
                        .amount(share)
                        .build());
            }
            return splits;
        }

        if (strategy == SplitStrategy.PERCENTAGE) {
            BigDecimal percentSum = BigDecimal.ZERO;
            for (SplitInput input : inputs) {
                if (input.percentage() == null) {
                    throw new IllegalArgumentException("percentage is required for PERCENTAGE splits");
                }
                percentSum = percentSum.add(input.percentage());
            }
            if (percentSum.compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new IllegalArgumentException("percentage splits must sum to 100, got " + percentSum);
            }

            List<ExpenseSplit> splits = new java.util.ArrayList<>();
            BigDecimal accumulated = BigDecimal.ZERO;
            for (int i = 0; i < inputs.size(); i++) {
                SplitInput input = inputs.get(i);
                BigDecimal share;
                if (i == inputs.size() - 1) {
                    share = total.subtract(accumulated).setScale(4, RoundingMode.HALF_UP);
                } else {
                    share = total.multiply(input.percentage())
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    accumulated = accumulated.add(share);
                }
                splits.add(ExpenseSplit.builder()
                        .participant(participants.get(input.participantId()))
                        .amount(share)
                        .percentage(input.percentage())
                        .build());
            }
            return splits;
        }

        if (strategy == SplitStrategy.EXACT) {
            BigDecimal exactSum = BigDecimal.ZERO;
            for (SplitInput input : inputs) {
                if (input.amount() == null) {
                    throw new IllegalArgumentException("amount is required for EXACT splits");
                }
                exactSum = exactSum.add(input.amount());
            }
            BigDecimal scaled = exactSum.setScale(total.scale(), RoundingMode.HALF_UP);
            if (scaled.compareTo(total) != 0) {
                throw new IllegalArgumentException("exact splits must sum to " + total + ", got " + scaled);
            }

            List<ExpenseSplit> splits = new java.util.ArrayList<>();
            for (SplitInput input : inputs) {
                splits.add(ExpenseSplit.builder()
                        .participant(participants.get(input.participantId()))
                        .amount(input.amount().setScale(4, RoundingMode.HALF_UP))
                        .build());
            }
            return splits;
        }

        throw new IllegalArgumentException("Unsupported split strategy: " + strategy);
    }

    public record SplitInput(Long participantId, BigDecimal percentage, BigDecimal amount) {}
}