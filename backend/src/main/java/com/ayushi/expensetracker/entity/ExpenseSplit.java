package com.ayushi.expensetracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits")
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // This user's share of the expense (already converted to group's default currency if needed)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    // Optional: which line item this share corresponds to, for itemized bills
    private String itemLabel;

    public ExpenseSplit() {}

    public Long getId() { return id; }
    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public void setShareAmount(BigDecimal shareAmount) { this.shareAmount = shareAmount; }
    public String getItemLabel() { return itemLabel; }
    public void setItemLabel(String itemLabel) { this.itemLabel = itemLabel; }
}
