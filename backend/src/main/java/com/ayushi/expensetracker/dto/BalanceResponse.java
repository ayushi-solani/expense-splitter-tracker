package com.ayushi.expensetracker.dto;

import java.math.BigDecimal;

// netBalance > 0: the group owes this person. netBalance < 0: this person owes the group.
public record BalanceResponse(Long userId, String email, BigDecimal netBalance) {
}
