package com.allobank.splitbill.repository;

import com.allobank.splitbill.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
}