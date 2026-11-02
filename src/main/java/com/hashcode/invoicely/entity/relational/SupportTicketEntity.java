package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 32)
    private String ticketNumber;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "reporter_user_id", nullable = false)
    private UUID reporterUserId;

    @Column(name = "assigned_admin_id")
    private UUID assignedAdminId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "priority", nullable = false, length = 16)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "category", nullable = false, length = 64)
    @Builder.Default
    private String category = "GENERAL";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
