package com.mohitjain.audit.consumer.repository;

import com.mohitjain.audit.consumer.entity.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    Optional<AuditRecord> findByEventId(String eventId);

    Page<AuditRecord> findBySource(String source, Pageable pageable);

    Page<AuditRecord> findBySourceAndAction(String source, String action, Pageable pageable);

    Page<AuditRecord> findByActorId(String actorId, Pageable pageable);

    Page<AuditRecord> findByPartitionYear(int partitionYear, Pageable pageable);

    long countByPartitionYear(int partitionYear);

    @Modifying
    @Query(value = "DELETE FROM audit_records WHERE partition_year = :year", nativeQuery = true)
    int deleteByPartitionYear(@Param("year") int year);
}
