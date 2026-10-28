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
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseEntity {

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

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "purchase_order_id")
    private UUID purchaseOrderId;

    @Column(name = "number", nullable = false, length = 128)
    private String number;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "foreign_amount", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal foreignAmount = BigDecimal.ZERO;

    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "currency_id", nullable = false, length = 8)
    @Builder.Default
    private String currencyId = "USD";

    @Column(name = "tax_name1", length = 64)
    private String taxName1;

    @Column(name = "tax_rate1", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxRate1 = BigDecimal.ZERO;

    @Column(name = "tax_name2", length = 64)
    private String taxName2;

    @Column(name = "tax_rate2", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxRate2 = BigDecimal.ZERO;

    @Column(name = "tax_name3", length = 64)
    private String taxName3;

    @Column(name = "tax_rate3", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxRate3 = BigDecimal.ZERO;

    @Column(name = "tax_amount1", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount1 = BigDecimal.ZERO;

    @Column(name = "tax_amount2", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount2 = BigDecimal.ZERO;

    @Column(name = "tax_amount3", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount3 = BigDecimal.ZERO;

    @Column(name = "uses_inclusive_taxes", nullable = false)
    @Builder.Default
    private boolean usesInclusiveTaxes = false;

    @Column(name = "should_be_invoiced", nullable = false)
    @Builder.Default
    private boolean shouldBeInvoiced = false;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_type_id", nullable = false, length = 32)
    @Builder.Default
    private String paymentTypeId = "1";

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(name = "public_notes", columnDefinition = "TEXT")
    private String publicNotes;

    @Column(name = "private_notes", columnDefinition = "TEXT")
    private String privateNotes;

    @Column(name = "custom_value1")
    private String customValue1;

    @Column(name = "custom_value2")
    private String customValue2;

    @Column(name = "custom_value3")
    private String customValue3;

    @Column(name = "custom_value4")
    private String customValue4;

    @Column(name = "status_id", nullable = false, length = 32)
    @Builder.Default
    private String statusId = "logged"; // logged, invoiced, paid, cancelled

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
