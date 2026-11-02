package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "table_column_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_company_table_columns", columnNames = {"company_id", "table_name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableColumnConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Column(name = "default_columns", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String defaultColumns = "[]";

    @Column(name = "locked_columns", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String lockedColumns = "[]";

    @Column(name = "restricted_for_employees", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String restrictedForEmployees = "[]";

    @Column(name = "allow_employee_customization", nullable = false)
    @Builder.Default
    private boolean allowEmployeeCustomization = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
