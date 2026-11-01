package com.ayushi.expensetracker.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Reduces a set of pairwise group balances to the minimum number of
 * settlement transactions using a greedy max-heap approach:
 *
 *  1. Compute each member's net balance (total paid - total owed).
 *  2. Repeatedly match the member who is owed the most against the
 *     member who owes the most, settling the smaller of the two amounts.
 *  3. Push back whichever balance remains, repeat until all balances are ~0.
 *
 * This does not preserve who-owes-whom-for-what, only who-should-pay-whom
 * how much to clear all debts with the fewest transactions.
 */
@Service
public class DebtSimplificationService {

    private static final BigDecimal EPSILON = new BigDecimal("0.01");

    public record Transaction(Long fromUserId, Long toUserId, BigDecimal amount) {}

    /**
     * @param netBalances userId -> net balance (positive = is owed money, negative = owes money)
     */
    public List<Transaction> simplify(Map<Long, BigDecimal> netBalances) {
        PriorityQueue<Balance> creditors = new PriorityQueue<>(
                Comparator.comparing((Balance b) -> b.amount).reversed());
        PriorityQueue<Balance> debtors = new PriorityQueue<>(
                Comparator.comparing((Balance b) -> b.amount).reversed());

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            BigDecimal amount = entry.getValue();
            if (amount.compareTo(EPSILON) > 0) {
                creditors.add(new Balance(entry.getKey(), amount));
            } else if (amount.compareTo(EPSILON.negate()) < 0) {
                debtors.add(new Balance(entry.getKey(), amount.abs()));
            }
        }

        List<Transaction> transactions = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Balance creditor = creditors.poll();
            Balance debtor = debtors.poll();

            BigDecimal settled = creditor.amount.min(debtor.amount);
            transactions.add(new Transaction(debtor.userId, creditor.userId, settled));

            BigDecimal creditorRemainder = creditor.amount.subtract(settled);
            BigDecimal debtorRemainder = debtor.amount.subtract(settled);

            if (creditorRemainder.compareTo(EPSILON) > 0) {
                creditors.add(new Balance(creditor.userId, creditorRemainder));
            }
            if (debtorRemainder.compareTo(EPSILON) > 0) {
                debtors.add(new Balance(debtor.userId, debtorRemainder));
            }
        }

        return transactions;
    }

    private record Balance(Long userId, BigDecimal amount) {}
}
