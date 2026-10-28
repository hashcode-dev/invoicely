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
@Table(name = "recurring_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringInvoiceEntity {

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

    @Column(name = "number", nullable = false, length = 128)
    private String number;

    @Column(name = "frequency_id", nullable = false, length = 32)
    @Builder.Default
    private String frequencyId = "monthly"; // weekly, monthly, quarterly, annually

    @Column(name = "status_id", nullable = false, length = 32)
    @Builder.Default
    private String statusId = "active"; // active, paused, completed

    @Column(name = "auto_bill", nullable = false)
    @Builder.Default
    private boolean autoBill = false;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "remaining_cycles", nullable = false)
    @Builder.Default
    private Integer remainingCycles = -1;

    @Column(name = "next_send_date")
    private LocalDate nextSendDate;

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
