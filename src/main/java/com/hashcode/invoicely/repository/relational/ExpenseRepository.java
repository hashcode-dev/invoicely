package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.ExpenseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, UUID>, JpaSpecificationExecutor<ExpenseEntity> {

    Optional<ExpenseEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<ExpenseEntity> findByCompanyIdAndNumber(UUID companyId, String number);

    List<ExpenseEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<ExpenseEntity> findByCompanyIdAndVendorId(UUID companyId, UUID vendorId, Pageable pageable);

    Page<ExpenseEntity> findByCompanyIdAndClientId(UUID companyId, UUID clientId, Pageable pageable);
}
