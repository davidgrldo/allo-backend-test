package com.allobank.splitbill.service;

import com.allobank.splitbill.model.Group;
import com.allobank.splitbill.model.Participant;
import com.allobank.splitbill.repository.GroupRepository;
import com.allobank.splitbill.repository.ParticipantRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final ParticipantRepository participantRepository;

    public GroupService(GroupRepository groupRepository, ParticipantRepository participantRepository) {
        this.groupRepository = groupRepository;
        this.participantRepository = participantRepository;
    }

    public Group create(String name, List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("A group must include at least one participant");
        }

        Set<Long> requestedIds = new HashSet<>(participantIds);
        List<Participant> found = participantRepository.findAllById(requestedIds);
        if (found.size() != requestedIds.size()) {
            Set<Long> foundIds = new HashSet<>();
            for (Participant p : found) {
                foundIds.add(p.getId());
            }
            requestedIds.removeAll(foundIds);
            throw new IllegalArgumentException("Unknown participant ids: " + requestedIds);
        }

        Group group = Group.builder()
                .name(name)
                .participants(new HashSet<>(found))
                .build();
        return groupRepository.save(group);
    }

    public List<Group> listAll() {
        return groupRepository.findAll();
    }

    public Group findById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Group not found: " + id));
    }
}