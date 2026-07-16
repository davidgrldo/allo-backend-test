package com.allobank.splitbill.service;

import com.allobank.splitbill.model.Group;
import com.allobank.splitbill.model.Participant;
import com.allobank.splitbill.model.Payment;
import com.allobank.splitbill.repository.GroupRepository;
import com.allobank.splitbill.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;

    public PaymentService(PaymentRepository paymentRepository, GroupRepository groupRepository) {
        this.paymentRepository = paymentRepository;
        this.groupRepository = groupRepository;
    }

    public Payment recordPayment(Long groupId, Long payerId, Long payeeId, BigDecimal amount) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));

        Map<Long, Participant> members = new HashMap<>();
        for (Participant p : group.getParticipants()) {
            members.put(p.getId(), p);
        }
        Participant payer = members.get(payerId);
        if (payer == null) {
            throw new IllegalArgumentException("Payer " + payerId + " is not a member of group " + groupId);
        }
        Participant payee = members.get(payeeId);
        if (payee == null) {
            throw new IllegalArgumentException("Payee " + payeeId + " is not a member of group " + groupId);
        }

        Payment payment = Payment.builder()
                .group(group)
                .payer(payer)
                .payee(payee)
                .amount(amount)
                .build();
        return paymentRepository.save(payment);
    }

    public List<Payment> listByGroup(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new EntityNotFoundException("Group not found: " + groupId);
        }
        return paymentRepository.findByGroupId(groupId);
    }
}