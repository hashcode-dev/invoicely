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
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "bank_integration_id")
    private UUID bankIntegrationId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_id", length = 8)
    @Builder.Default
    private String currencyId = "USD";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_type", nullable = false, length = 32)
    @Builder.Default
    private String baseType = "debit"; // debit (expense) or credit (payment)

    @Column(name = "status_id", nullable = false, length = 32)
    @Builder.Default
    private String statusId = "unmatched"; // unmatched, matched, converted

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "bank_transaction_rule_id")
    private UUID bankTransactionRuleId;

    @Column(name = "participant")
    private String participant;

    @Column(name = "participant_name")
    private String participantName;

    @Column(name = "invoice_ids")
    private String invoiceIds;

    @Column(name = "account_type", length = 64)
    private String accountType;

    @Column(name = "category_type", length = 64)
    private String categoryType;

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
