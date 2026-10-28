package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "subdomain", unique = true, length = 128)
    private String subdomain;

    @Column(name = "currency_id", nullable = false, length = 8)
    @Builder.Default
    private String currencyId = "USD";

    @Column(name = "country_id", nullable = false, length = 8)
    @Builder.Default
    private String countryId = "US";

    @Column(name = "size_id", length = 32)
    private String sizeId;

    @Column(name = "industry_id", length = 64)
    private String industryId;

    @Column(name = "vat_number", length = 64)
    private String vatNumber;

    @Column(name = "gst_number", length = 64)
    private String gstNumber;

    @Column(name = "portal_domain")
    private String portalDomain;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
