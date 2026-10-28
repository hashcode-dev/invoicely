package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.VendorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<VendorEntity, UUID>, JpaSpecificationExecutor<VendorEntity> {

    Optional<VendorEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<VendorEntity> findByCompanyIdAndVendorNumber(UUID companyId, String vendorNumber);

    List<VendorEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<VendorEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
