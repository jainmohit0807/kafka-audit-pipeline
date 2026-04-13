package com.mohitjain.audit.consumer.service;

import com.mohitjain.audit.consumer.entity.AuditRecord;
import com.mohitjain.audit.consumer.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final AuditRecordRepository repository;

    public Page<AuditRecord> getDeadLetters(Pageable pageable) {
        return repository.findBySourceAndAction("DLQ", "DEAD_LETTER", pageable);
    }

    public long countDeadLetters() {
        return repository.findBySourceAndAction("DLQ", "DEAD_LETTER",
                Pageable.unpaged()).getTotalElements();
    }
}
