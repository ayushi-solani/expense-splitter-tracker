package com.ayushi.expensetracker.dto;

import java.math.BigDecimal;

public record SplitResponse(String userEmail, BigDecimal shareAmount, String itemLabel) {
}