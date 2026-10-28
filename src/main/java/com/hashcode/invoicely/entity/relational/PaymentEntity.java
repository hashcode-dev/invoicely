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
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "company_gateway_id")
    private UUID companyGatewayId;

    @Column(name = "number", nullable = false, length = 128)
    private String number;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "refunded", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal refunded = BigDecimal.ZERO;

    @Column(name = "applied", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal applied = BigDecimal.ZERO;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "type_id", nullable = false, length = 32)
    @Builder.Default
    private String typeId = "bank_transfer";

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(name = "private_notes", columnDefinition = "TEXT")
    private String privateNotes;

    @Column(name = "status_id", nullable = false, length = 32)
    @Builder.Default
    private String statusId = "completed";

    @Column(name = "is_manual", nullable = false)
    @Builder.Default
    private boolean isManual = true;

    @Column(name = "custom_value1")
    private String customValue1;

    @Column(name = "custom_value2")
    private String customValue2;

    @Column(name = "custom_value3")
    private String customValue3;

    @Column(name = "custom_value4")
    private String customValue4;

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

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaymentableEntity> paymentables = new ArrayList<>();

    public void addPaymentable(PaymentableEntity paymentable) {
        paymentables.add(paymentable);
        paymentable.setPayment(this);
    }

    public void removePaymentable(PaymentableEntity paymentable) {
        paymentables.remove(paymentable);
        paymentable.setPayment(null);
    }
}
