package com.mohitjain.audit.producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohitjain.audit.producer.dto.AuditEventRequest;
import com.mohitjain.audit.producer.dto.AuditEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.topic}")
    private String auditTopic;

    public AuditEventResponse publish(AuditEventRequest request) {
        String eventId = UUID.randomUUID().toString();
        String key = request.getSource() + ":" + request.getActorId();

        try {
            var envelope = new java.util.HashMap<>(objectMapper.convertValue(request, java.util.Map.class));
            envelope.put("eventId", eventId);
            String payload = objectMapper.writeValueAsString(envelope);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(auditTopic, key, payload);

            SendResult<String, String> result = future.join();
            RecordMetadata metadata = result.getRecordMetadata();

            log.info("Published audit event {} to {}[{}]@{}",
                    eventId, metadata.topic(), metadata.partition(), metadata.offset());

            return AuditEventResponse.builder()
                    .eventId(eventId)
                    .topic(metadata.topic())
                    .partition(metadata.partition())
                    .offset(metadata.offset())
                    .publishedAt(Instant.now())
                    .status("ACCEPTED")
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event {}: {}", eventId, e.getMessage());
            throw new IllegalArgumentException("Invalid audit event payload", e);
        }
    }
}
