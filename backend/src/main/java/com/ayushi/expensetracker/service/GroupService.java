package com.ayushi.expensetracker.service;

import com.ayushi.expensetracker.dto.CreateGroupRequest;
import com.ayushi.expensetracker.dto.GroupResponse;
import com.ayushi.expensetracker.entity.Group;
import com.ayushi.expensetracker.entity.User;
import com.ayushi.expensetracker.repository.GroupRepository;
import com.ayushi.expensetracker.repository.UserRepository;
import  org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository){
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public GroupResponse createGroup(CreateGroupRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Group group = new Group(request.getName());
        group.setDefaultCurrency(request.getDefaultCurrency());
        group.getMembers().add(creator);

        Group saved = groupRepository.save(group);
        return toResponse(saved);
    }

    public GroupResponse addMember(Long groupId, String memberEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("User with that email not found"));

        group.getMembers().add(member);
        Group saved = groupRepository.save(group);
        return toResponse(saved);
    }

    public GroupResponse getGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        return toResponse(group);
    }

    public List<GroupResponse> getGroupsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return user.getGroups().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private GroupResponse toResponse(Group group) {
        List<String> emails = group.getMembers().stream()
                .map(User::getEmail)
                .collect(Collectors.toList());
        return new GroupResponse(group.getId(), group.getName(), group.getDefaultCurrency(), emails);
    }
}
