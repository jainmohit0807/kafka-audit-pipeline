package com.mohitjain.audit.consumer;

import com.mohitjain.audit.consumer.listener.AuditEventListener;
import com.mohitjain.audit.consumer.service.AuditPersistenceService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditPersistenceService persistenceService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private AuditEventListener listener;

    @Test
    void onAuditEvent_persistsAndAcknowledges() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "audit-events", 0, 100L, "key",
                "{\"source\":\"test\",\"action\":\"LOGIN\",\"resourceType\":\"session\"}");

        listener.onAuditEvent(record, acknowledgment);

        verify(persistenceService).persist(record);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onAuditEvent_whenPersistFails_throwsWithoutAck() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "audit-events", 0, 101L, "key", "bad-json");

        doThrow(new RuntimeException("parse error"))
                .when(persistenceService).persist(any());

        try {
            listener.onAuditEvent(record, acknowledgment);
        } catch (RuntimeException ignored) {
        }

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void onDeadLetter_persistsAndAcknowledges() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "audit-events-dlq", 0, 1L, "key", "failed-payload");

        listener.onDeadLetter(record, acknowledgment);

        verify(persistenceService).persistDeadLetter(record);
        verify(acknowledgment).acknowledge();
    }
}
