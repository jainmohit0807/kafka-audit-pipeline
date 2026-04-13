package com.mohitjain.audit.consumer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohitjain.audit.consumer.entity.AuditRecord;
import com.mohitjain.audit.consumer.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditPersistenceService {

    private final AuditRecordRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void persist(ConsumerRecord<String, String> record) {
        try {
            JsonNode node = objectMapper.readTree(record.value());

            String eventId = node.has("eventId") ? node.get("eventId").asText() : UUID.randomUUID().toString();

            AuditRecord entity = AuditRecord.builder()
                    .eventId(eventId)
                    .source(node.path("source").asText())
                    .action(node.path("action").asText())
                    .actorId(node.path("actorId").asText(null))
                    .actorEmail(node.path("actorEmail").asText(null))
                    .resourceType(node.path("resourceType").asText())
                    .resourceId(node.path("resourceId").asText(null))
                    .ipAddress(node.path("ipAddress").asText(null))
                    .userAgent(node.path("userAgent").asText(null))
                    .outcome(node.path("outcome").asText("SUCCESS"))
                    .metadata(node.has("metadata") ? node.get("metadata").toString() : null)
                    .partitionYear(Year.now().getValue())
                    .processedAt(Instant.now())
                    .kafkaTopic(record.topic())
                    .kafkaPartition(record.partition())
                    .kafkaOffset(record.offset())
                    .build();

            repository.save(entity);
            log.debug("Persisted audit record {} for source={} action={}",
                    entity.getEventId(), entity.getSource(), entity.getAction());

        } catch (Exception e) {
            log.error("Failed to parse/persist audit event: {}", e.getMessage(), e);
            throw new RuntimeException("Audit event persistence failed", e);
        }
    }

    @Transactional
    public void persistDeadLetter(ConsumerRecord<String, String> record) {
        AuditRecord entity = AuditRecord.builder()
                .eventId(UUID.randomUUID().toString())
                .source("DLQ")
                .action("DEAD_LETTER")
                .resourceType("kafka-message")
                .outcome("FAILED")
                .metadata(record.value())
                .partitionYear(Year.now().getValue())
                .processedAt(Instant.now())
                .kafkaTopic(record.topic())
                .kafkaPartition(record.partition())
                .kafkaOffset(record.offset())
                .build();

        repository.save(entity);
        log.warn("Persisted dead letter record {}", entity.getEventId());
    }
}
