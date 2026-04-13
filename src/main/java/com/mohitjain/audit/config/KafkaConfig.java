package com.mohitjain.audit.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Value("${audit.kafka.topic}")
    private String auditTopic;

    @Value("${audit.kafka.dlq-topic}")
    private String dlqTopic;

    @Value("${audit.kafka.partitions:6}")
    private int partitions;

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(auditTopic)
                .partitions(partitions)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (record, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}
