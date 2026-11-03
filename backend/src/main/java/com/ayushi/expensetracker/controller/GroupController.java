package com.ayushi.expensetracker.controller;

import com.ayushi.expensetracker.dto.AddMemberRequest;
import com.ayushi.expensetracker.dto.CreateGroupRequest;
import com.ayushi.expensetracker.dto.GroupResponse;
import com.ayushi.expensetracker.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(groupService.createGroup(request, email));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupResponse> addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        return ResponseEntity.ok(groupService.addMember(groupId, request.getEmail()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(groupService.getGroupsForUser(email));
    }
}