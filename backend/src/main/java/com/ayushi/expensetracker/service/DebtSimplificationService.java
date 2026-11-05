package com.ayushi.expensetracker.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Reduces group balances to the minimum number of settlement transactions.
 *
 * Key idea: if a set of people's balances sum to zero, they can settle among
 * themselves with (size - 1) payments. So the fewest payments overall is
 * (number of people) - (maximum number of disjoint zero-sum groups).
 *
 * Finding that maximum is NP-hard in general, so:
 *  - up to EXACT_LIMIT people: exact answer using a bitmask DP over subsets
 *  - above that: greedy fallback (at most n-1 payments, usually near-optimal)
 *
 * All arithmetic is done in whole cents (long) to avoid rounding drift.
 */
@Service
public class DebtSimplificationService {

    static final int EXACT_LIMIT = 16;

    public record Transaction(Long fromUserId, Long toUserId, BigDecimal amount) {}

    private record Party(int index, long cents) {}

    /**
     * @param netBalances userId -> net balance (positive = is owed money, negative = owes money)
     */
    public List<Transaction> simplify(Map<Long, BigDecimal> netBalances) {
        // Sorted by user id so results are deterministic; zero balances drop out
        List<Long> ids = new ArrayList<>();
        List<Long> centsList = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : new TreeMap<>(netBalances).entrySet()) {
            long cents = toCents(e.getValue());
            if (cents != 0) {
                ids.add(e.getKey());
                centsList.add(cents);
            }
        }

        int n = ids.size();
        if (n == 0) {
            return List.of();
        }

        long[] balances = new long[n];
        for (int i = 0; i < n; i++) {
            balances[i] = centsList.get(i);
        }

        List<List<Integer>> groups;
        if (n <= EXACT_LIMIT) {
            groups = partitionIntoZeroSumGroups(balances);
        } else {
            List<Integer> everyone = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                everyone.add(i);
            }
            groups = List.of(everyone);
        }

        List<Transaction> transactions = new ArrayList<>();
        for (List<Integer> group : groups) {
            settleGreedily(group, ids, balances, transactions);
        }
        return transactions;
    }

    /**
     * Splits people into the maximum number of disjoint groups whose balances sum to zero.
     *
     * dp[mask] = best count of zero-sum groups when the people in mask are placed in some
     * order: dp[mask] = max over i in mask of dp[mask without i], plus 1 if sum[mask] == 0.
     * Walking the chosen order back from the full set, every prefix with sum zero marks
     * the end of one group.
     */
    private List<List<Integer>> partitionIntoZeroSumGroups(long[] balances) {
        int n = balances.length;
        int full = (1 << n) - 1;

        long[] sum = new long[1 << n];
        for (int mask = 1; mask <= full; mask++) {
            int low = Integer.numberOfTrailingZeros(mask);
            sum[mask] = sum[mask & (mask - 1)] + balances[low];
        }

        int[] dp = new int[1 << n];
        int[] last = new int[1 << n];
        for (int mask = 1; mask <= full; mask++) {
            int best = -1;
            int bestPerson = -1;
            for (int i = 0; i < n; i++) {
                if ((mask >> i & 1) == 0) {
                    continue;
                }
                int value = dp[mask ^ (1 << i)];
                if (value > best) {
                    best = value;
                    bestPerson = i;
                }
            }
            dp[mask] = best + (sum[mask] == 0 ? 1 : 0);
            last[mask] = bestPerson;
        }

        // Walk the chain of masks, recording every zero-sum prefix as a group boundary
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(full);
        int mask = full;
        while (mask != 0) {
            mask ^= 1 << last[mask];
            if (mask != 0 && sum[mask] == 0) {
                boundaries.add(mask);
            }
        }
        boundaries.add(0);

        List<List<Integer>> groups = new ArrayList<>();
        for (int k = 0; k + 1 < boundaries.size(); k++) {
            int groupMask = boundaries.get(k) ^ boundaries.get(k + 1);
            List<Integer> members = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((groupMask >> i & 1) == 1) {
                    members.add(i);
                }
            }
            if (!members.isEmpty()) {
                groups.add(members);
            }
        }
        return groups;
    }

    /** Matches the biggest creditor with the biggest debtor until the group is settled. */
    private void settleGreedily(List<Integer> group, List<Long> ids, long[] balances,
                                List<Transaction> out) {
        Comparator<Party> biggestFirst = Comparator
                .comparingLong(Party::cents).reversed()
                .thenComparingInt(Party::index);

        PriorityQueue<Party> creditors = new PriorityQueue<>(biggestFirst);
        PriorityQueue<Party> debtors = new PriorityQueue<>(biggestFirst);

        for (int index : group) {
            long cents = balances[index];
            if (cents > 0) {
                creditors.add(new Party(index, cents));
            } else if (cents < 0) {
                debtors.add(new Party(index, -cents));
            }
        }

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Party creditor = creditors.poll();
            Party debtor = debtors.poll();

            long settled = Math.min(creditor.cents(), debtor.cents());
            out.add(new Transaction(ids.get(debtor.index()), ids.get(creditor.index()),
                    BigDecimal.valueOf(settled, 2)));

            if (creditor.cents() > settled) {
                creditors.add(new Party(creditor.index(), creditor.cents() - settled));
            }
            if (debtor.cents() > settled) {
                debtors.add(new Party(debtor.index(), debtor.cents() - settled));
            }
        }
    }

    private static long toCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}