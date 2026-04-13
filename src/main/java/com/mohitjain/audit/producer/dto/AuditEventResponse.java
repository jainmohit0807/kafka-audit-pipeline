package com.mohitjain.audit.producer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventResponse {

    private String eventId;

    private String topic;

    private int partition;

    private long offset;

    private Instant publishedAt;

    private String status;
}
