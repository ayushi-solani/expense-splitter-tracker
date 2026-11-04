package com.ayushi.expensetracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record ItemRequest(String label, BigDecimal amount,List<String> participantEmails) {
}
