package com.allobank.splitbill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantResponse {

    private Long id;
    private String name;
    private Instant createdAt;
}