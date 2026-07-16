package com.allobank.splitbill.controller;

import com.allobank.splitbill.dto.CreateGroupRequest;
import com.allobank.splitbill.dto.GroupResponse;
import com.allobank.splitbill.dto.ParticipantResponse;
import com.allobank.splitbill.model.Group;
import com.allobank.splitbill.model.Participant;
import com.allobank.splitbill.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@Valid @RequestBody CreateGroupRequest request) {
        Group group = groupService.create(request.getName(), request.getParticipantIds());
        return toResponse(group);
    }

    @GetMapping
    public List<GroupResponse> list() {
        return groupService.listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public GroupResponse get(@PathVariable Long id) {
        return toResponse(groupService.findById(id));
    }

    private GroupResponse toResponse(Group group) {
        List<ParticipantResponse> participants = group.getParticipants().stream()
                .map(p -> ParticipantResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .participants(participants)
                .createdAt(group.getCreatedAt())
                .build();
    }
}