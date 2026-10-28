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
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "status_id")
    private UUID statusId;

    @Column(name = "number", length = 128)
    private String number;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration", nullable = false)
    @Builder.Default
    private Long duration = 0L; // Duration in seconds

    @Column(name = "is_running", nullable = false)
    @Builder.Default
    private Boolean isRunning = false;

    @Column(name = "time_log", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String timeLog = "[]";

    @Column(name = "rate", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "is_date_based", nullable = false)
    @Builder.Default
    private Boolean isDateBased = false;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "calculated_start_date")
    private Instant calculatedStartDate;

    @Column(name = "status_order", nullable = false)
    @Builder.Default
    private Integer statusOrder = 0;

    @Column(name = "status_sort_order", nullable = false)
    @Builder.Default
    private Integer statusSortOrder = 0;

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
