package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorEntity {

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

    @Column(name = "vendor_number", nullable = false, length = 64)
    private String vendorNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "website")
    private String website;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "currency_id", nullable = false, length = 8)
    @Builder.Default
    private String currencyId = "USD";

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "city", length = 128)
    private String city;

    @Column(name = "state", length = 128)
    private String state;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(name = "country_id", length = 8)
    private String countryId;

    @Column(name = "vat_number", length = 64)
    private String vatNumber;

    @Column(name = "id_number", length = 64)
    private String idNumber;

    @Column(name = "routing_id", length = 64)
    private String routingId;

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

    @Column(name = "is_tax_exempt", nullable = false)
    @Builder.Default
    private boolean isTaxExempt = false;

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

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VendorContactEntity> contacts = new ArrayList<>();

    public void addContact(VendorContactEntity contact) {
        contacts.add(contact);
        contact.setVendor(this);
    }

    public void removeContact(VendorContactEntity contact) {
        contacts.remove(contact);
        contact.setVendor(null);
    }
}
