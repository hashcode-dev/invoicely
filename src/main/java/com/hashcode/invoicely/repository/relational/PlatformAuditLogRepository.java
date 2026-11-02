package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.PlatformAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlatformAuditLogRepository extends JpaRepository<PlatformAuditLogEntity, UUID> {
    Page<PlatformAuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
