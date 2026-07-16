package com.allobank.splitbill.controller;

import com.allobank.splitbill.dto.PaymentResponse;
import com.allobank.splitbill.dto.RecordPaymentRequest;
import com.allobank.splitbill.model.Payment;
import com.allobank.splitbill.service.PaymentService;
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
@RequestMapping("/api/v1/groups/{groupId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse record(@PathVariable Long groupId, @Valid @RequestBody RecordPaymentRequest request) {
        Payment payment = paymentService.recordPayment(groupId, request.getPayerId(),
                request.getPayeeId(), request.getAmount());
        return toResponse(payment);
    }

    @GetMapping
    public List<PaymentResponse> list(@PathVariable Long groupId) {
        return paymentService.listByGroup(groupId).stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .groupId(payment.getGroup().getId())
                .payerId(payment.getPayer().getId())
                .payerName(payment.getPayer().getName())
                .payeeId(payment.getPayee().getId())
                .payeeName(payment.getPayee().getName())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}