package com.mohitjain.audit.retention;

import com.mohitjain.audit.config.RetentionPolicyProperties;
import com.mohitjain.audit.consumer.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Enforces the configurable retention policy by dropping audit records
 * older than the configured retention window. Runs on a cron schedule
 * (default: midnight on January 1 each year).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetentionScheduler {

    private final RetentionPolicyProperties retentionProperties;
    private final AuditRecordRepository repository;

    @Scheduled(cron = "${audit.retention.partition-check-cron:0 0 0 1 1 *}")
    @Transactional
    public void enforceRetentionPolicy() {
        if (!retentionProperties.isAutoDropExpiredPartitions()) {
            log.info("Auto-drop of expired partitions is disabled");
            return;
        }

        int currentYear = Year.now().getValue();
        int cutoffYear = currentYear - retentionProperties.getYearsToRetain();

        log.info("Retention policy check: retaining {} years, cutoff year = {}",
                retentionProperties.getYearsToRetain(), cutoffYear);

        for (int year = cutoffYear; year >= 2000; year--) {
            long count = repository.countByPartitionYear(year);
            if (count == 0) {
                break;
            }
            int deleted = repository.deleteByPartitionYear(year);
            log.info("Dropped {} audit records from partition year {}", deleted, year);
        }
    }

    public String getRetentionSummary() {
        int currentYear = Year.now().getValue();
        int cutoffYear = currentYear - retentionProperties.getYearsToRetain();
        long totalRecords = repository.count();

        return String.format("Retention: %d years | Cutoff: %d | Total records: %d",
                retentionProperties.getYearsToRetain(), cutoffYear, totalRecords);
    }
}
