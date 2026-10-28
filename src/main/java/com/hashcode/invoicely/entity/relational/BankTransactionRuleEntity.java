package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_transaction_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "applies_to", length = 64)
    @Builder.Default
    private String appliesTo = "all";

    @Column(name = "rules", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String rules = "[]";

    @Column(name = "auto_convert", nullable = false)
    @Builder.Default
    private Boolean autoConvert = false;

    @Column(name = "matches_on_all", nullable = false)
    @Builder.Default
    private Boolean matchesOnAll = true;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "client_id")
    private UUID clientId;

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
