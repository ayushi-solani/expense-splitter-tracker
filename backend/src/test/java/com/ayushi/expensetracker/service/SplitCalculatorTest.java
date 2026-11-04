package com.ayushi.expensetracker.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitCalculatorTest {

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void equalSplitGivesLeftoverCentsToFirstPeopleAndSumsExactly() {
        Map<Long, BigDecimal> shares = SplitCalculator.splitEqually(
                new BigDecimal("100.00"), List.of(1L, 2L, 3L));

        assertMoney("33.34", shares.get(1L));
        assertMoney("33.33", shares.get(2L));
        assertMoney("33.33", shares.get(3L));

        BigDecimal sum = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMoney("100.00", sum);
    }

    @Test
    void itemizedSplitSharesExtraChargesProportionally() {
        // Pizza 300 shared by users 1 and 2, drink 100 for user 1 only, extra 40
        // Subtotals: user 1 = 250, user 2 = 150, so extra splits 25 and 15
        List<SplitCalculator.Item> items = List.of(
                new SplitCalculator.Item("Pizza", new BigDecimal("300.00"), List.of(1L, 2L)),
                new SplitCalculator.Item("Drink", new BigDecimal("100.00"), List.of(1L))
        );

        List<SplitCalculator.ShareLine> lines =
                SplitCalculator.splitItemized(items, new BigDecimal("40.00"));

        BigDecimal user1 = lines.stream().filter(l -> l.userId().equals(1L))
                .map(SplitCalculator.ShareLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal user2 = lines.stream().filter(l -> l.userId().equals(2L))
                .map(SplitCalculator.ShareLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertMoney("275.00", user1);
        assertMoney("165.00", user2);
        assertMoney("440.00", user1.add(user2));
    }
}