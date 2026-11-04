package com.ayushi.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExpenseResponse {
    private Long id;
    private Long groupId;
    private String paidByEmail;
    private String description;
    private BigDecimal amount;
    private String currency;
    private String category;
    private LocalDate expenseDate;
    private List<SplitResponse> splits;

    public ExpenseResponse(Long id, Long groupId, String paidByEmail, String description,
                           BigDecimal amount, String currency, String category, LocalDate expenseDate, List<SplitResponse> splits) {
        this.id = id;
        this.groupId = groupId;
        this.paidByEmail = paidByEmail;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.expenseDate = expenseDate;
        this.splits = splits;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getPaidByEmail() { return paidByEmail; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCategory() { return category; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public List<SplitResponse> getSplits(){return splits;}
}

