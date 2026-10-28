package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "number", nullable = false, length = 128)
    private String number;

    @Column(name = "po_number", length = 128)
    private String poNumber;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status_id", nullable = false, length = 32)
    @Builder.Default
    private String statusId = "draft"; // draft, sent, received, converted_to_expense, cancelled

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "paid_to_date", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal paidToDate = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "is_amount_discount", nullable = false)
    @Builder.Default
    private boolean isAmountDiscount = false;

    @Column(name = "total_taxes", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalTaxes = BigDecimal.ZERO;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "public_notes", columnDefinition = "TEXT")
    private String publicNotes;

    @Column(name = "private_notes", columnDefinition = "TEXT")
    private String privateNotes;

    @Column(name = "footer", columnDefinition = "TEXT")
    private String footer;

    @Column(name = "uses_inclusive_taxes", nullable = false)
    @Builder.Default
    private boolean usesInclusiveTaxes = false;

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

    @Column(name = "custom_value1")
    private String customValue1;

    @Column(name = "custom_value2")
    private String customValue2;

    @Column(name = "custom_value3")
    private String customValue3;

    @Column(name = "custom_value4")
    private String customValue4;

    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "design_id", nullable = false, length = 32)
    @Builder.Default
    private String designId = "clean";

    @Column(name = "expense_id")
    private UUID expenseId;

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

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<PurchaseOrderItemEntity> lineItems = new ArrayList<>();

    public void addLineItem(PurchaseOrderItemEntity item) {
        lineItems.add(item);
        item.setPurchaseOrder(this);
    }

    public void removeLineItem(PurchaseOrderItemEntity item) {
        lineItems.remove(item);
        item.setPurchaseOrder(null);
    }
}
