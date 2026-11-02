package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "target_account_id")
    private UUID targetAccountId;

    @Column(name = "target_company_id")
    private UUID targetCompanyId;

    @Column(name = "ip_address", nullable = false, length = 45)
    @Builder.Default
    private String ipAddress = "127.0.0.1";

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
