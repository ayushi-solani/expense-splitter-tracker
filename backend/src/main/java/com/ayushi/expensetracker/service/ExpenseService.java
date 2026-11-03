package com.ayushi.expensetracker.service;

import com.ayushi.expensetracker.dto.CreateExpenseRequest;
import com.ayushi.expensetracker.dto.ExpenseResponse;
import com.ayushi.expensetracker.entity.Expense;
import com.ayushi.expensetracker.entity.Group;
import com.ayushi.expensetracker.entity.User;
import com.ayushi.expensetracker.repository.ExpenseRepository;
import com.ayushi.expensetracker.repository.GroupRepository;
import com.ayushi.expensetracker.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupRepository groupRepository,
                          UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public ExpenseResponse createExpense(CreateExpenseRequest request, String email) {
        User user = findUser(email);

        Expense expense = new Expense();
        expense.setPaidBy(user);
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency());
        expense.setCategory(request.getCategory());
        expense.setExpenseDate(request.getExpenseDate());

        if (request.getGroupId() != null) {
            Group group = findGroup(request.getGroupId());
            requireMember(group, user);
            expense.setGroup(group);
        }

        return toResponse(expenseRepository.save(expense));
    }

    public List<ExpenseResponse> getMyExpenses(String email) {
        User user = findUser(email);
        return expenseRepository.findByPaidByIdOrderByExpenseDateDesc(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ExpenseResponse> getGroupExpenses(Long groupId, String email) {
        User user = findUser(email);
        Group group = findGroup(groupId);
        requireMember(group, user);

        return expenseRepository.findByGroupId(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Group findGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }

    private void requireMember(Group group, User user) {
        boolean isMember = group.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isMember) {
            throw new IllegalArgumentException("You are not a member of this group");
        }
    }

    private ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getGroup() != null ? e.getGroup().getId() : null,
                e.getPaidBy().getEmail(),
                e.getDescription(),
                e.getAmount(),
                e.getCurrency(),
                e.getCategory(),
                e.getExpenseDate()
        );
    }
}