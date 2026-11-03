package com.ayushi.expensetracker.controller;

import com.ayushi.expensetracker.dto.CreateExpenseRequest;
import com.ayushi.expensetracker.dto.ExpenseResponse;
import com.ayushi.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(expenseService.createExpense(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getMyExpenses(Authentication authentication) {
        return ResponseEntity.ok(expenseService.getMyExpenses(authentication.getName()));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ExpenseResponse>> getGroupExpenses(
            @PathVariable Long groupId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(expenseService.getGroupExpenses(groupId, authentication.getName()));
    }
}
