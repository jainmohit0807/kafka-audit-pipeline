package com.mohitjain.audit.producer.controller;

import com.mohitjain.audit.consumer.entity.AuditRecord;
import com.mohitjain.audit.consumer.repository.AuditRecordRepository;
import com.mohitjain.audit.producer.dto.AuditEventRequest;
import com.mohitjain.audit.producer.dto.AuditEventResponse;
import com.mohitjain.audit.producer.service.AuditEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/audit-events")
@RequiredArgsConstructor
@Tag(name = "Audit Events", description = "Produce and query audit events")
public class AuditEventController {

    private final AuditEventPublisher publisher;
    private final AuditRecordRepository repository;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Publish an audit event to Kafka")
    public AuditEventResponse publishEvent(@Valid @RequestBody AuditEventRequest request) {
        return publisher.publish(request);
    }

    @GetMapping
    @Operation(summary = "Query persisted audit records with pagination")
    public Page<AuditRecord> listEvents(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String action,
            @PageableDefault(size = 20) Pageable pageable) {
        if (source != null && action != null) {
            return repository.findBySourceAndAction(source, action, pageable);
        } else if (source != null) {
            return repository.findBySource(source, pageable);
        }
        return repository.findAll(pageable);
    }

    @GetMapping("/count")
    @Operation(summary = "Count total persisted audit records")
    public long countEvents() {
        return repository.count();
    }
}
