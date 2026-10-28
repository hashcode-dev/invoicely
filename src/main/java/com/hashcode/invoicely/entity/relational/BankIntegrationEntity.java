package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_integrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankIntegrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "bank_account_name", nullable = false)
    private String bankAccountName;

    @Column(name = "bank_account_number", length = 128)
    private String bankAccountNumber;

    @Column(name = "bank_account_type", length = 64)
    @Builder.Default
    private String bankAccountType = "checking";

    @Column(name = "bank_account_status", length = 64)
    @Builder.Default
    private String bankAccountStatus = "active";

    @Column(name = "currency", length = 8)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "balance", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "provider_name", length = 128)
    @Builder.Default
    private String providerName = "manual";

    @Column(name = "integration_type", length = 64)
    @Builder.Default
    private String integrationType = "plaid";

    @Column(name = "nordigen_institution_id", length = 128)
    private String nordigenInstitutionId;

    @Column(name = "auto_sync", nullable = false)
    @Builder.Default
    private Boolean autoSync = true;

    @Column(name = "disabled_upstream", nullable = false)
    @Builder.Default
    private Boolean disabledUpstream = false;

    @Column(name = "from_date")
    private LocalDate fromDate;

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
