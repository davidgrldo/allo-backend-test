package com.allobank.splitbill.service;

import com.allobank.splitbill.model.Participant;
import com.allobank.splitbill.repository.ParticipantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    public ParticipantService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    public Participant create(String name) {
        Participant participant = Participant.builder().name(name).build();
        return participantRepository.save(participant);
    }

    public List<Participant> listAll() {
        return participantRepository.findAll();
    }

    public Participant findById(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Participant not found: " + id));
    }
}