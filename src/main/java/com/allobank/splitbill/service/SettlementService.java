package com.allobank.splitbill.service;

import com.allobank.splitbill.config.ServiceChargeProperties;
import com.allobank.splitbill.model.Expense;
import com.allobank.splitbill.model.ExpenseSplit;
import com.allobank.splitbill.model.Group;
import com.allobank.splitbill.model.Participant;
import com.allobank.splitbill.model.Payment;
import com.allobank.splitbill.repository.ExpenseRepository;
import com.allobank.splitbill.repository.GroupRepository;
import com.allobank.splitbill.repository.PaymentRepository;
import com.allobank.splitbill.util.ServiceChargeCalculator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settlement engine: computes net balances per participant from expenses and
 * recorded payments, then minimizes the number of settle-up transactions via a
 * greedy creditor/debtor netting algorithm. Includes a personalized service
 * charge and per-category expense summaries.
 */
@Service
public class SettlementService {

    private static final int RESPONSE_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(RESPONSE_SCALE, RoundingMode.HALF_UP);
    private static final BigDecimal EPSILON = new BigDecimal("0.005");

    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;
    private final ServiceChargeProperties serviceChargeProperties;

    public SettlementService(ExpenseRepository expenseRepository,
                             PaymentRepository paymentRepository,
                             GroupRepository groupRepository,
                             ServiceChargeProperties serviceChargeProperties) {
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
        this.groupRepository = groupRepository;
        this.serviceChargeProperties = serviceChargeProperties;
    }

    public SettlementResult computeSettlement(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));

        Map<Long, Participant> participants = new LinkedHashMap<>();
        for (Participant p : group.getParticipants()) {
            participants.put(p.getId(), p);
        }

        Map<Long, BigDecimal> net = new HashMap<>();
        for (Long id : participants.keySet()) {
            net.put(id, BigDecimal.ZERO);
        }

        BigDecimal totalExpenses = BigDecimal.ZERO;
        Map<String, CategoryAggregate> categoryAggregates = new HashMap<>();

        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        for (Expense expense : expenses) {
            BigDecimal amount = round(expense.getAmount());
            totalExpenses = totalExpenses.add(amount);

            Long payerId = expense.getPayer().getId();
            net.merge(payerId, amount, BigDecimal::add);

            for (ExpenseSplit split : expense.getSplits()) {
                BigDecimal owed = round(split.getAmount());
                Long participantId = split.getParticipant().getId();
                net.merge(participantId, owed.negate(), BigDecimal::add);
            }

            String categoryKey = expense.getCategory() == null ? "uncategorized" : expense.getCategory().name();
            CategoryAggregate agg = categoryAggregates.computeIfAbsent(categoryKey,
                    k -> new CategoryAggregate(BigDecimal.ZERO, 0));
            categoryAggregates.put(categoryKey, new CategoryAggregate(agg.total().add(amount), agg.count() + 1));
        }

        List<Payment> payments = paymentRepository.findByGroupId(groupId);
        for (Payment payment : payments) {
            BigDecimal amount = round(payment.getAmount());
            net.merge(payment.getPayer().getId(), amount, BigDecimal::add);
            net.merge(payment.getPayee().getId(), amount.negate(), BigDecimal::add);
        }

        int serviceChargePct = ServiceChargeCalculator.computePct(serviceChargeProperties.getGithubUsername());
        BigDecimal serviceChargeAmount = ServiceChargeCalculator.computeAmount(totalExpenses, serviceChargePct);

        List<ParticipantBalance> balances = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : net.entrySet()) {
            balances.add(new ParticipantBalance(e.getKey(),
                    nameOf(participants, e.getKey()),
                    round(e.getValue())));
        }

        List<TransactionEntry> transactions = greedyNet(new ArrayList<>(balances));

        List<CategorySummary> summaries = new ArrayList<>();
        categoryAggregates.forEach((category, agg) ->
                summaries.add(new CategorySummary(category, round(agg.total()), agg.count())));

        return new SettlementResult(
                groupId,
                round(totalExpenses),
                serviceChargePct,
                serviceChargeAmount,
                balances,
                transactions,
                summaries
        );
    }

    private List<TransactionEntry> greedyNet(List<ParticipantBalance> balances) {
        List<ParticipantBalance> creditors = new ArrayList<>();
        List<ParticipantBalance> debtors = new ArrayList<>();
        for (ParticipantBalance b : balances) {
            BigDecimal bal = b.netBalance();
            if (bal.compareTo(EPSILON) > 0) {
                creditors.add(new ParticipantBalance(b.participantId(), b.name(), bal));
            } else if (bal.compareTo(EPSILON.negate()) < 0) {
                debtors.add(new ParticipantBalance(b.participantId(), b.name(), bal));
            }
        }

        Comparator<ParticipantBalance> byAbsValueDesc = Comparator.comparing(
                ParticipantBalance::netBalance, Comparator.comparingInt(BigDecimal::signum)
        ).thenComparing(ParticipantBalance::netBalance);
        creditors.sort(Comparator.comparing(ParticipantBalance::netBalance).reversed());
        debtors.sort(Comparator.comparing(ParticipantBalance::netBalance));

        List<TransactionEntry> result = new ArrayList<>();
        int ci = 0;
        int di = 0;
        while (ci < creditors.size() && di < debtors.size()) {
            ParticipantBalance creditor = creditors.get(ci);
            ParticipantBalance debtor = debtors.get(di);
            BigDecimal credit = creditor.netBalance();
            BigDecimal debt = debtor.netBalance().abs();
            BigDecimal amount = credit.min(debt);

            if (amount.compareTo(EPSILON) > 0) {
                result.add(new TransactionEntry(
                        debtor.participantId(), debtor.name(),
                        creditor.participantId(), creditor.name(),
                        round(amount)
                ));
            }

            BigDecimal newCredit = credit.subtract(amount);
            BigDecimal newDebt = debtor.netBalance().add(amount);

            creditors.set(ci, new ParticipantBalance(creditor.participantId(), creditor.name(), newCredit));
            debtors.set(di, new ParticipantBalance(debtor.participantId(), debtor.name(), newDebt));

            if (creditors.get(ci).netBalance().compareTo(EPSILON) <= 0) ci++;
            if (debtors.get(di).netBalance().compareTo(EPSILON.negate()) >= 0) di++;
        }
        return result;
    }

    private String nameOf(Map<Long, Participant> participants, Long id) {
        Participant p = participants.get(id);
        return p == null ? "" : p.getName();
    }

    private BigDecimal round(BigDecimal value) {
        if (value == null) return ZERO;
        return value.setScale(RESPONSE_SCALE, RoundingMode.HALF_UP);
    }

    private record CategoryAggregate(BigDecimal total, int count) {}

    public record ParticipantBalance(Long participantId, String name, BigDecimal netBalance) {}

    public record TransactionEntry(Long fromParticipantId, String fromName,
                                    Long toParticipantId, String toName, BigDecimal amount) {}

    public record CategorySummary(String category, BigDecimal totalAmount, int expenseCount) {}

    public record SettlementResult(Long groupId,
                                   BigDecimal totalExpenses,
                                   int serviceChargePct,
                                   BigDecimal serviceChargeAmount,
                                   List<ParticipantBalance> balances,
                                   List<TransactionEntry> transactions,
                                   List<CategorySummary> categorySummaries) {}
}