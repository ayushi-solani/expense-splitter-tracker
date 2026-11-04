package com.ayushi.expensetracker.service;

import com.ayushi.expensetracker.dto.CreateExpenseRequest;
import com.ayushi.expensetracker.dto.ExpenseResponse;
import com.ayushi.expensetracker.dto.ItemRequest;
import com.ayushi.expensetracker.dto.SplitResponse;
import com.ayushi.expensetracker.dto.SplitType;
import com.ayushi.expensetracker.entity.Expense;
import com.ayushi.expensetracker.entity.ExpenseSplit;
import com.ayushi.expensetracker.entity.Group;
import com.ayushi.expensetracker.entity.User;
import com.ayushi.expensetracker.repository.ExpenseRepository;
import com.ayushi.expensetracker.repository.GroupRepository;
import com.ayushi.expensetracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, String email) {
        User payer = findUser(email);

        Expense expense = new Expense();
        expense.setPaidBy(payer);
        expense.setDescription(request.getDescription());
        expense.setCategory(request.getCategory());
        expense.setExpenseDate(request.getExpenseDate());

        // Personal expense: no group, no splits
        if (request.getGroupId() == null) {
            expense.setCurrency(currencyOrDefault(request.getCurrency(), "INR"));
            expense.setAmount(requireAmount(request));
            return toResponse(expenseRepository.save(expense));
        }

        Group group = findGroup(request.getGroupId());
        requireMember(group, payer);

        String currency = currencyOrDefault(request.getCurrency(), group.getDefaultCurrency());
        if (!currency.equalsIgnoreCase(group.getDefaultCurrency())) {
            throw new IllegalArgumentException("Expense currency must match the group's currency ("
                    + group.getDefaultCurrency() + "). Multi-currency is not supported yet.");
        }
        expense.setGroup(group);
        expense.setCurrency(currency);

        // Sorted by id so "all members" splits are deterministic
        List<User> members = group.getMembers().stream()
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());
        Map<String, User> membersByEmail = new LinkedHashMap<>();
        Map<Long, User> membersById = new LinkedHashMap<>();
        for (User m : members) {
            membersByEmail.put(m.getEmail(), m);
            membersById.put(m.getId(), m);
        }

        SplitType type = request.getSplitType() == null ? SplitType.EQUAL : request.getSplitType();
        List<SplitCalculator.ShareLine> lines;
        BigDecimal total;

        if (type == SplitType.EQUAL) {
            total = requireAmount(request);
            List<Long> participantIds = resolveParticipants(request.getSplitAmongEmails(), membersByEmail);
            lines = SplitCalculator.splitEqually(total, participantIds).entrySet().stream()
                    .map(e -> new SplitCalculator.ShareLine(e.getKey(), e.getValue(), null))
                    .collect(Collectors.toList());
        } else {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new IllegalArgumentException("An itemized split needs at least one item");
            }

            BigDecimal extra = request.getExtraCharges() == null
                    ? BigDecimal.ZERO : money(request.getExtraCharges());
            if (extra.signum() < 0) {
                throw new IllegalArgumentException("Extra charges cannot be negative");
            }

            List<SplitCalculator.Item> items = new ArrayList<>();
            BigDecimal itemsTotal = BigDecimal.ZERO;
            for (ItemRequest item : request.getItems()) {
                if (item.amount() == null || item.amount().signum() <= 0) {
                    throw new IllegalArgumentException("Each item needs a positive amount");
                }
                BigDecimal itemAmount = money(item.amount());
                List<Long> ids = resolveParticipants(item.participantEmails(), membersByEmail);
                items.add(new SplitCalculator.Item(item.label(), itemAmount, ids));
                itemsTotal = itemsTotal.add(itemAmount);
            }

            total = itemsTotal.add(extra);
            if (request.getAmount() != null && money(request.getAmount()).compareTo(total) != 0) {
                throw new IllegalArgumentException("Amount " + money(request.getAmount())
                        + " does not match items plus extra charges (" + total + ")");
            }
            lines = SplitCalculator.splitItemized(items, extra);
        }

        expense.setAmount(total);
        for (SplitCalculator.ShareLine line : lines) {
            ExpenseSplit split = new ExpenseSplit();
            split.setExpense(expense);
            split.setUser(membersById.get(line.userId()));
            split.setShareAmount(line.amount());
            split.setItemLabel(line.itemLabel());
            expense.getSplits().add(split);
        }

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getMyExpenses(String email) {
        User user = findUser(email);
        return expenseRepository.findByPaidByIdOrderByExpenseDateDesc(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(Long groupId, String email) {
        User user = findUser(email);
        Group group = findGroup(groupId);
        requireMember(group, user);

        return expenseRepository.findByGroupId(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private List<Long> resolveParticipants(List<String> emails, Map<String, User> membersByEmail) {
        if (emails == null || emails.isEmpty()) {
            return membersByEmail.values().stream().map(User::getId).collect(Collectors.toList());
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String participantEmail : emails) {
            User member = membersByEmail.get(participantEmail);
            if (member == null) {
                throw new IllegalArgumentException(participantEmail + " is not a member of this group");
            }
            ids.add(member.getId());
        }
        return new ArrayList<>(ids);
    }

    private BigDecimal requireAmount(CreateExpenseRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Amount is required and must be positive");
        }
        return money(request.getAmount());
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String currencyOrDefault(String currency, String fallback) {
        return (currency == null || currency.isBlank()) ? fallback : currency.toUpperCase();
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
        List<SplitResponse> splits = e.getSplits().stream()
                .map(s -> new SplitResponse(s.getUser().getEmail(), s.getShareAmount(), s.getItemLabel()))
                .collect(Collectors.toList());

        return new ExpenseResponse(
                e.getId(),
                e.getGroup() != null ? e.getGroup().getId() : null,
                e.getPaidBy().getEmail(),
                e.getDescription(),
                e.getAmount(),
                e.getCurrency(),
                e.getCategory(),
                e.getExpenseDate(),
                splits
        );
    }
}