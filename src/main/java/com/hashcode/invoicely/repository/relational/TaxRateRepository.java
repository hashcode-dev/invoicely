package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.TaxRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRateEntity, UUID>, JpaSpecificationExecutor<TaxRateEntity> {

    List<TaxRateEntity> findByCompanyIdAndIsDeletedFalse(UUID companyId);

    Optional<TaxRateEntity> findByCompanyIdAndId(UUID companyId, UUID id);
}
