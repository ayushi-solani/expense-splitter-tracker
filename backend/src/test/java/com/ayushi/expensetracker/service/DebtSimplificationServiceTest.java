package com.ayushi.expensetracker.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebtSimplificationServiceTest {

    private final DebtSimplificationService service = new DebtSimplificationService();

    @Test
    void simplifiesThreeWayDebtIntoMinimumTransactions() {
        // A owes 300, B owes 200, C is owed 500 total -> should collapse to 2 transactions
        Map<Long, BigDecimal> balances = Map.of(
                1L, new BigDecimal("-300.00"), // A owes 300
                2L, new BigDecimal("-200.00"), // B owes 200
                3L, new BigDecimal("500.00")   // C is owed 500
        );

        List<DebtSimplificationService.Transaction> result = service.simplify(balances);

        assertEquals(2, result.size());
        BigDecimal total = result.stream()
                .map(DebtSimplificationService.Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("500.00"), total);
    }

    @Test
    void returnsNoTransactionsWhenAllBalancesAreZero() {
        Map<Long, BigDecimal> balances = Map.of(1L, BigDecimal.ZERO, 2L, BigDecimal.ZERO);
        assertEquals(0, service.simplify(balances).size());
    }
}
