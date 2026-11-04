package com.ayushi.expensetracker.controller;

import com.ayushi.expensetracker.dto.BalanceResponse;
import com.ayushi.expensetracker.dto.SettlementSuggestion;
import com.ayushi.expensetracker.service.BalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balances")
    public ResponseEntity<List<BalanceResponse>> getBalances(
            @PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(balanceService.getBalances(groupId, authentication.getName()));
    }

    @GetMapping("/settlements/suggested")
    public ResponseEntity<List<SettlementSuggestion>> suggestSettlements(
            @PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(balanceService.suggestSettlements(groupId, authentication.getName()));
    }
}
