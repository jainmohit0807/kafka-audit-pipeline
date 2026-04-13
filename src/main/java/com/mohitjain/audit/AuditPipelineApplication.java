package com.mohitjain.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class AuditPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditPipelineApplication.class, args);
    }
}
