package com.mohitjain.audit.consumer.listener;

import com.mohitjain.audit.consumer.service.AuditPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditPersistenceService persistenceService;

    @KafkaListener(
            topics = "${audit.kafka.topic}",
            groupId = "${audit.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAuditEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received audit event from {}[{}]@{} key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            persistenceService.persist(record);
            ack.acknowledge();
            log.debug("Successfully persisted and acknowledged event at offset {}", record.offset());
        } catch (Exception e) {
            log.error("Failed to process audit event at offset {}: {}",
                    record.offset(), e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "${audit.kafka.dlq-topic}",
            groupId = "${audit.kafka.consumer-group}-dlq"
    )
    public void onDeadLetter(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.warn("Dead letter received from {}[{}]@{}: {}",
                record.topic(), record.partition(), record.offset(), record.value());
        persistenceService.persistDeadLetter(record);
        ack.acknowledge();
    }
}
