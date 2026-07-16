package com.allobank.splitbill.repository;

import com.allobank.splitbill.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}