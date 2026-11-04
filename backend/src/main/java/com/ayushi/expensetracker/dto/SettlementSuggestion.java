package com.ayushi.expensetracker.dto;

import java.math.BigDecimal;

public record SettlementSuggestion(String fromEmail, String toEmail, BigDecimal amount) {
}