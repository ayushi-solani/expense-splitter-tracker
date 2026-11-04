package com.ayushi.expensetracker.service;

import com.ayushi.expensetracker.dto.BalanceResponse;
import com.ayushi.expensetracker.dto.SettlementSuggestion;
import com.ayushi.expensetracker.entity.Expense;
import com.ayushi.expensetracker.entity.ExpenseSplit;
import com.ayushi.expensetracker.entity.Group;
import com.ayushi.expensetracker.entity.Settlement;
import com.ayushi.expensetracker.entity.User;
import com.ayushi.expensetracker.repository.ExpenseRepository;
import com.ayushi.expensetracker.repository.GroupRepository;
import com.ayushi.expensetracker.repository.SettlementRepository;
import com.ayushi.expensetracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BalanceService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final DebtSimplificationService debtSimplificationService;

    public BalanceService(GroupRepository groupRepository,
                          UserRepository userRepository,
                          ExpenseRepository expenseRepository,
                          SettlementRepository settlementRepository,
                          DebtSimplificationService debtSimplificationService) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.debtSimplificationService = debtSimplificationService;
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalances(Long groupId, String email) {
        Group group = loadGroupForMember(groupId, email);
        Map<Long, BigDecimal> net = computeNetBalances(group);

        return group.getMembers().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(m -> new BalanceResponse(m.getId(), m.getEmail(),
                        net.getOrDefault(m.getId(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SettlementSuggestion> suggestSettlements(Long groupId, String email) {
        Group group = loadGroupForMember(groupId, email);
        Map<Long, BigDecimal> net = computeNetBalances(group);

        Map<Long, String> emails = new HashMap<>();
        for (User m : group.getMembers()) {
            emails.put(m.getId(), m.getEmail());
        }

        return debtSimplificationService.simplify(net).stream()
                .map(t -> new SettlementSuggestion(
                        emails.get(t.fromUserId()), emails.get(t.toUserId()), t.amount()))
                .collect(Collectors.toList());
    }

    /**
     * net = (what you paid) - (your share of expenses), adjusted for settlements already made.
     * Positive: the group owes you. Negative: you owe the group.
     */
    private Map<Long, BigDecimal> computeNetBalances(Group group) {
        Map<Long, BigDecimal> net = new HashMap<>();
        for (User m : group.getMembers()) {
            net.put(m.getId(), BigDecimal.ZERO);
        }

        for (Expense e : expenseRepository.findByGroupId(group.getId())) {
            net.merge(e.getPaidBy().getId(), e.getAmount(), BigDecimal::add);
            for (ExpenseSplit s : e.getSplits()) {
                net.merge(s.getUser().getId(), s.getShareAmount().negate(), BigDecimal::add);
            }
        }

        // A settlement from A to B means A paid B back: A owes less, B is owed less
        for (Settlement s : settlementRepository.findByGroupId(group.getId())) {
            net.merge(s.getFromUser().getId(), s.getAmount(), BigDecimal::add);
            net.merge(s.getToUser().getId(), s.getAmount().negate(), BigDecimal::add);
        }
        return net;
    }

    private Group loadGroupForMember(Long groupId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        boolean isMember = group.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isMember) {
            throw new IllegalArgumentException("You are not a member of this group");
        }
        return group;
    }
}
