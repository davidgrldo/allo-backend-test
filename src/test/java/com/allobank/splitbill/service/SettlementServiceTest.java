package com.allobank.splitbill.service;

import com.allobank.splitbill.config.ServiceChargeProperties;
import com.allobank.splitbill.model.Expense;
import com.allobank.splitbill.model.ExpenseSplit;
import com.allobank.splitbill.model.Group;
import com.allobank.splitbill.model.Participant;
import com.allobank.splitbill.model.Payment;
import com.allobank.splitbill.model.enums.ExpenseCategory;
import com.allobank.splitbill.model.enums.SplitStrategy;
import com.allobank.splitbill.repository.ExpenseRepository;
import com.allobank.splitbill.repository.GroupRepository;
import com.allobank.splitbill.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettlementServiceTest {

    private ExpenseRepository expenseRepository;
    private PaymentRepository paymentRepository;
    private GroupRepository groupRepository;
    private ServiceChargeProperties properties;
    private SettlementService settlementService;

    private Participant a;
    private Participant b;
    private Participant c;
    private Group group;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        groupRepository = mock(GroupRepository.class);
        properties = mock(ServiceChargeProperties.class);

        settlementService = new SettlementService(expenseRepository, paymentRepository, groupRepository, properties);

        a = Participant.builder().id(1L).name("A").build();
        b = Participant.builder().id(2L).name("B").build();
        c = Participant.builder().id(3L).name("C").build();
        group = Group.builder().id(10L).name("Trip")
                .participants(new HashSet<>(List.of(a, b, c)))
                .build();

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(properties.getGithubUsername()).thenReturn("allobankdev");
    }

    private Expense expense(Long id, Participant payer, BigDecimal amount,
                            SplitStrategy strategy, List<ExpenseSplit> splits,
                            ExpenseCategory category) {
        Expense expense = Expense.builder()
                .id(id)
                .group(group)
                .payer(payer)
                .amount(amount)
                .splitStrategy(strategy)
                .category(category)
                .splits(splits)
                .build();
        for (ExpenseSplit split : splits) {
            split.setExpense(expense);
        }
        return expense;
    }

    private ExpenseSplit split(Participant p, BigDecimal amount) {
        return ExpenseSplit.builder().participant(p).amount(amount).build();
    }

    @Test
    void settlementMinimizesTransactionsAndMatchesExpectedAmounts() {
        BigDecimal ninety = new BigDecimal("90.00");
        Expense e1 = expense(1L, a, ninety, SplitStrategy.EQUAL,
                List.of(split(a, new BigDecimal("30.0000")),
                        split(b, new BigDecimal("30.0000")),
                        split(c, new BigDecimal("30.0000"))),
                ExpenseCategory.FOOD);
        Expense e2 = expense(2L, b, new BigDecimal("30.00"), SplitStrategy.EQUAL,
                List.of(split(b, new BigDecimal("15.0000")),
                        split(c, new BigDecimal("15.0000"))),
                ExpenseCategory.TRANSPORT);

        when(expenseRepository.findByGroupId(10L)).thenReturn(List.of(e1, e2));
        when(paymentRepository.findByGroupId(10L)).thenReturn(List.of());

        SettlementService.SettlementResult result = settlementService.computeSettlement(10L);

        assertThat(result.balances()).containsExactlyInAnyOrder(
                new SettlementService.ParticipantBalance(1L, "A", new BigDecimal("60.00")),
                new SettlementService.ParticipantBalance(2L, "B", new BigDecimal("-15.00")),
                new SettlementService.ParticipantBalance(3L, "C", new BigDecimal("-45.00"))
        );

        assertThat(result.transactions()).hasSize(2);
        assertThat(result.transactions()).satisfiesExactlyInAnyOrder(
                tx -> {
                    assertThat(tx.fromParticipantId()).isEqualTo(3L);
                    assertThat(tx.toParticipantId()).isEqualTo(1L);
                    assertThat(tx.amount()).isEqualByComparingTo("45.00");
                },
                tx -> {
                    assertThat(tx.fromParticipantId()).isEqualTo(2L);
                    assertThat(tx.toParticipantId()).isEqualTo(1L);
                    assertThat(tx.amount()).isEqualByComparingTo("15.00");
                }
        );

        assertThat(result.serviceChargePct()).isEqualTo(5);
        assertThat(result.totalExpenses()).isEqualByComparingTo("120.00");
        assertThat(result.serviceChargeAmount()).isEqualByComparingTo("6.00");

        assertThat(result.categorySummaries()).containsExactlyInAnyOrder(
                new SettlementService.CategorySummary("FOOD", new BigDecimal("90.00"), 1),
                new SettlementService.CategorySummary("TRANSPORT", new BigDecimal("30.00"), 1)
        );
    }

    @Test
    void settlementIsIdempotent() {
        BigDecimal ninety = new BigDecimal("90.00");
        Expense e1 = expense(1L, a, ninety, SplitStrategy.EQUAL,
                List.of(split(a, new BigDecimal("30.0000")),
                        split(b, new BigDecimal("30.0000")),
                        split(c, new BigDecimal("30.0000"))),
                null);
        when(expenseRepository.findByGroupId(10L)).thenReturn(List.of(e1));
        when(paymentRepository.findByGroupId(10L)).thenReturn(List.of());

        SettlementService.SettlementResult first = settlementService.computeSettlement(10L);
        SettlementService.SettlementResult second = settlementService.computeSettlement(10L);

        assertThat(second.transactions()).isEqualTo(first.transactions());
        assertThat(second.serviceChargeAmount()).isEqualTo(first.serviceChargeAmount());
        assertThat(second.balances()).isEqualTo(first.balances());
    }

    @Test
    void recordedPaymentsReduceOutstandingBalances() {
        BigDecimal ninety = new BigDecimal("90.00");
        Expense e1 = expense(1L, a, ninety, SplitStrategy.EQUAL,
                List.of(split(a, new BigDecimal("30.0000")),
                        split(b, new BigDecimal("30.0000")),
                        split(c, new BigDecimal("30.0000"))),
                ExpenseCategory.FOOD);
        when(expenseRepository.findByGroupId(10L)).thenReturn(List.of(e1));

        Payment payment = Payment.builder()
                .id(1L)
                .group(group)
                .payer(c)
                .payee(a)
                .amount(new BigDecimal("30.00"))
                .build();
        when(paymentRepository.findByGroupId(10L)).thenReturn(List.of(payment));

        SettlementService.SettlementResult result = settlementService.computeSettlement(10L);

        assertThat(result.balances()).containsExactlyInAnyOrder(
                new SettlementService.ParticipantBalance(1L, "A", new BigDecimal("30.00")),
                new SettlementService.ParticipantBalance(2L, "B", new BigDecimal("-30.00")),
                new SettlementService.ParticipantBalance(3L, "C", new BigDecimal("0.00"))
        );

        assertThat(result.transactions()).hasSize(1);
        assertThat(result.transactions().get(0).fromParticipantId()).isEqualTo(2L);
        assertThat(result.transactions().get(0).toParticipantId()).isEqualTo(1L);
        assertThat(result.transactions().get(0).amount()).isEqualByComparingTo("30.00");
    }

    @Test
    void uncategorizedExpensesBucketedAsUncategorized() {
        Expense e1 = expense(1L, a, new BigDecimal("30.00"), SplitStrategy.EXACT,
                List.of(split(a, new BigDecimal("30.0000"))),
                null);
        Expense e2 = expense(2L, b, new BigDecimal("60.00"), SplitStrategy.EXACT,
                List.of(split(b, new BigDecimal("30.0000")),
                        split(c, new BigDecimal("30.0000"))),
                ExpenseCategory.FOOD);

        when(expenseRepository.findByGroupId(10L)).thenReturn(List.of(e1, e2));
        when(paymentRepository.findByGroupId(10L)).thenReturn(List.of());

        SettlementService.SettlementResult result = settlementService.computeSettlement(10L);

        assertThat(result.categorySummaries()).contains(
                new SettlementService.CategorySummary("uncategorized", new BigDecimal("30.00"), 1),
                new SettlementService.CategorySummary("FOOD", new BigDecimal("60.00"), 1)
        );
    }
}