package com.ayushi.expensetracker.repository;

import com.ayushi.expensetracker.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {
    List<ExpenseSplit> findByExpenseGroupId(Long groupId);
    List<ExpenseSplit> findByUserId(Long userId);
}
