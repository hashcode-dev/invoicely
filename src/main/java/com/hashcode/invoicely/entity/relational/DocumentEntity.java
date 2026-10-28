package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false, length = 64)
    @Builder.Default
    private String type = "application/pdf";

    @Column(name = "size", nullable = false)
    @Builder.Default
    private Long size = 0L;

    @Column(name = "width", nullable = false)
    @Builder.Default
    private Integer width = 0;

    @Column(name = "height", nullable = false)
    @Builder.Default
    private Integer height = 0;

    @Column(name = "hash", nullable = false, length = 64)
    private String hash;

    @Column(name = "disk", nullable = false, length = 32)
    @Builder.Default
    private String disk = "local";

    @Column(name = "url", length = 512)
    private String url;

    @Column(name = "preview", columnDefinition = "TEXT")
    private String preview;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
