package com.ayushi.expensetracker.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebtSimplificationServiceTest {

    private final DebtSimplificationService service = new DebtSimplificationService();

    private static Map<Long, BigDecimal> balances(double... values) {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            map.put((long) (i + 1), BigDecimal.valueOf(values[i]).setScale(2));
        }
        return map;
    }

    /** Applying every transaction must bring every balance back to zero. */
    private static void assertAllSettled(Map<Long, BigDecimal> original,
                                         List<DebtSimplificationService.Transaction> transactions) {
        Map<Long, BigDecimal> net = new HashMap<>(original);
        for (DebtSimplificationService.Transaction t : transactions) {
            net.merge(t.fromUserId(), t.amount(), BigDecimal::add);
            net.merge(t.toUserId(), t.amount().negate(), BigDecimal::add);
        }
        for (BigDecimal value : net.values()) {
            assertEquals(0, value.compareTo(BigDecimal.ZERO));
        }
    }

    @Test
    void simplifiesThreeWayDebtIntoTwoTransactions() {
        Map<Long, BigDecimal> input = balances(-300, -200, 500);

        List<DebtSimplificationService.Transaction> result = service.simplify(input);

        assertEquals(2, result.size());
        assertAllSettled(input, result);
    }

    @Test
    void returnsNoTransactionsWhenAllBalancesAreZero() {
        assertEquals(0, service.simplify(balances(0, 0)).size());
    }

    @Test
    void separatesIndependentPairsIntoOneTransactionEach() {
        // Two pairs that cancel on their own: 2 payments, not 3
        Map<Long, BigDecimal> input = balances(10, -10, 20, -20);

        List<DebtSimplificationService.Transaction> result = service.simplify(input);

        assertEquals(2, result.size());
        assertAllSettled(input, result);
    }

    @Test
    void findsTrueMinimumWhereGreedyAloneGivesSix() {
        // Greedy matching needs 6 payments here. The true minimum is 5.
        Map<Long, BigDecimal> input = balances(-5, 9, 8, -6, 1, -8, 4, -3);

        List<DebtSimplificationService.Transaction> result = service.simplify(input);

        assertEquals(5, result.size());
        assertAllSettled(input, result);
    }

    @Test
    void fallsBackToGreedyForLargeGroupsAndStillSettlesEveryone() {
        Map<Long, BigDecimal> input = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (long id = 1; id <= 19; id++) {
            BigDecimal value = BigDecimal.valueOf((id % 7) - 3).setScale(2);
            input.put(id, value);
            total = total.add(value);
        }
        input.put(20L, total.negate());

        List<DebtSimplificationService.Transaction> result = service.simplify(input);

        assertAllSettled(input, result);
        // Greedy never needs more than (people - 1) payments
        assertEquals(true, result.size() <= 19);
    }
}