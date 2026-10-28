package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.PurchaseOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, UUID>, JpaSpecificationExecutor<PurchaseOrderEntity> {

    Optional<PurchaseOrderEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<PurchaseOrderEntity> findByCompanyIdAndNumber(UUID companyId, String number);

    List<PurchaseOrderEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<PurchaseOrderEntity> findByCompanyIdAndVendorId(UUID companyId, UUID vendorId, Pageable pageable);
}
