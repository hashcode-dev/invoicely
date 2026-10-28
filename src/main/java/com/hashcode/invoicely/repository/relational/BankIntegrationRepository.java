package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.BankIntegrationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankIntegrationRepository extends JpaRepository<BankIntegrationEntity, UUID>, JpaSpecificationExecutor<BankIntegrationEntity> {

    Optional<BankIntegrationEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<BankIntegrationEntity> findByCompanyIdAndBankAccountName(UUID companyId, String name);

    List<BankIntegrationEntity> findByCompanyIdAndIsDeletedFalse(UUID companyId);

    List<BankIntegrationEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<BankIntegrationEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
