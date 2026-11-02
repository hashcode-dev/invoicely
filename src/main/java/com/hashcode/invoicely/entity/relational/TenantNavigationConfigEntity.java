package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_navigation_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_company_role_nav_item", columnNames = {"company_id", "role", "item_key"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantNavigationConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "role", nullable = false, length = 32)
    @Builder.Default
    private String role = "DEFAULT";

    @Column(name = "item_key", nullable = false, length = 64)
    private String itemKey;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private boolean isVisible = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "custom_label", length = 64)
    private String customLabel;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean isLocked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
