package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

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

    @Column(name = "product_key", nullable = false, length = 128)
    private String productKey;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cost", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "price", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

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

    @Column(name = "in_stock_quantity", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal inStockQuantity = BigDecimal.ZERO;

    @Column(name = "stock_notification", nullable = false)
    @Builder.Default
    private boolean stockNotification = false;

    @Column(name = "stock_notification_threshold", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal stockNotificationThreshold = BigDecimal.ZERO;

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
