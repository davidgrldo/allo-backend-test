package com.allobank.splitbill.controller;

import com.allobank.splitbill.dto.CreateParticipantRequest;
import com.allobank.splitbill.dto.ParticipantResponse;
import com.allobank.splitbill.model.Participant;
import com.allobank.splitbill.service.ParticipantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantResponse create(@Valid @RequestBody CreateParticipantRequest request) {
        Participant participant = participantService.create(request.getName());
        return toResponse(participant);
    }

    @GetMapping
    public List<ParticipantResponse> list() {
        return participantService.listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private ParticipantResponse toResponse(Participant p) {
        return ParticipantResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .createdAt(p.getCreatedAt())
                .build();
    }
}