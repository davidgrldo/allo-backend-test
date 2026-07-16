package com.allobank.splitbill.repository;

import com.allobank.splitbill.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByGroupId(Long groupId);
}