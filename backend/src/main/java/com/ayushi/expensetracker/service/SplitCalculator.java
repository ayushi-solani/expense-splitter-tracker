package com.ayushi.expensetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure money-splitting logic. All work is done in whole cents (long) so that
 * the shares always add up exactly to the total, with no rounding drift.
 */
public final class SplitCalculator {

    public static final String EXTRA_LABEL = "Tax/tip";

    private SplitCalculator() {}

    public record Item(String label, BigDecimal amount, List<Long> participantIds) {}

    public record ShareLine(Long userId, BigDecimal amount, String itemLabel) {}

    /** Splits total as evenly as possible. Leftover cents go to the first people in the list. */
    public static Map<Long, BigDecimal> splitEqually(BigDecimal total, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }
        long cents = toCents(total);
        if (cents <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        int n = userIds.size();
        long base = cents / n;
        long remainder = cents % n;

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            long share = base + (i < remainder ? 1 : 0);
            result.put(userIds.get(i), fromCents(share));
        }
        return result;
    }

    /**
     * Each item is split equally among its participants. Extra charges (tax/tip)
     * are then shared in proportion to each person's item subtotal, using the
     * largest-remainder method so the extra adds up exactly.
     */
    public static List<ShareLine> splitItemized(List<Item> items, BigDecimal extraCharges) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }

        List<ShareLine> lines = new ArrayList<>();
        Map<Long, Long> subtotalCents = new LinkedHashMap<>();

        for (Item item : items) {
            Map<Long, BigDecimal> shares = splitEqually(item.amount(), item.participantIds());
            for (Map.Entry<Long, BigDecimal> e : shares.entrySet()) {
                lines.add(new ShareLine(e.getKey(), e.getValue(), item.label()));
                subtotalCents.merge(e.getKey(), toCents(e.getValue()), Long::sum);
            }
        }

        long extraCents = extraCharges == null ? 0 : toCents(extraCharges);
        if (extraCents < 0) {
            throw new IllegalArgumentException("Extra charges cannot be negative");
        }
        if (extraCents == 0) {
            return lines;
        }

        long totalSubtotal = subtotalCents.values().stream().mapToLong(Long::longValue).sum();

        Map<Long, Long> extraShare = new LinkedHashMap<>();
        Map<Long, BigDecimal> fraction = new LinkedHashMap<>();
        long allocated = 0;

        for (Map.Entry<Long, Long> e : subtotalCents.entrySet()) {
            BigDecimal exact = BigDecimal.valueOf(extraCents)
                    .multiply(BigDecimal.valueOf(e.getValue()))
                    .divide(BigDecimal.valueOf(totalSubtotal), 10, RoundingMode.DOWN);
            long floor = exact.setScale(0, RoundingMode.DOWN).longValue();
            extraShare.put(e.getKey(), floor);
            fraction.put(e.getKey(), exact.subtract(BigDecimal.valueOf(floor)));
            allocated += floor;
        }

        // Hand out the leftover cents to whoever lost the most to rounding
        long leftover = extraCents - allocated;
        List<Long> byFraction = new ArrayList<>(fraction.keySet());
        byFraction.sort((a, b) -> fraction.get(b).compareTo(fraction.get(a)));
        for (int i = 0; i < leftover; i++) {
            extraShare.merge(byFraction.get(i % byFraction.size()), 1L, Long::sum);
        }

        for (Map.Entry<Long, Long> e : extraShare.entrySet()) {
            if (e.getValue() > 0) {
                lines.add(new ShareLine(e.getKey(), fromCents(e.getValue()), EXTRA_LABEL));
            }
        }
        return lines;
    }

    private static long toCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}