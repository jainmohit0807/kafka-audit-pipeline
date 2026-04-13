package com.mohitjain.audit.consumer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "audit_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String action;

    private String actorId;

    private String actorEmail;

    @Column(nullable = false)
    private String resourceType;

    private String resourceId;

    private String ipAddress;

    private String userAgent;

    @Column(nullable = false)
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private int partitionYear;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant processedAt;

    @Column(name = "kafka_topic")
    private String kafkaTopic;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;
}
