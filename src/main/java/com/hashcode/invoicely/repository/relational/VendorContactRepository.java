package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.VendorContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorContactRepository extends JpaRepository<VendorContactEntity, UUID> {

    List<VendorContactEntity> findByVendorId(UUID vendorId);
}
