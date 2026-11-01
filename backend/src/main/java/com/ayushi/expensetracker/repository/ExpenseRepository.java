package com.ayushi.expensetracker.repository;

import com.ayushi.expensetracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupId(Long groupId);
    List<Expense> findByPaidByIdAndExpenseDateBetween(Long userId, LocalDate start, LocalDate end);
    List<Expense> findByPaidByIdAndCategoryAndExpenseDateBetween(
            Long userId, String category, LocalDate start, LocalDate end);
}
