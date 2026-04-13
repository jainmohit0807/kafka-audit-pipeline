package com.mohitjain.audit.producer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventRequest {

    @NotBlank(message = "Source system is required")
    private String source;

    @NotBlank(message = "Action is required")
    private String action;

    @NotNull(message = "Actor is required")
    private String actorId;

    private String actorEmail;

    @NotBlank(message = "Resource type is required")
    private String resourceType;

    private String resourceId;

    private String ipAddress;

    private String userAgent;

    private String outcome;

    private Map<String, Object> metadata;
}
