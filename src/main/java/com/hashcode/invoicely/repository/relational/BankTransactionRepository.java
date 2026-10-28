package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.BankTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransactionEntity, UUID>, JpaSpecificationExecutor<BankTransactionEntity> {

    Optional<BankTransactionEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<BankTransactionEntity> findByCompanyIdAndBankIntegrationIdAndIsDeletedFalse(UUID companyId, UUID bankIntegrationId);

    List<BankTransactionEntity> findByCompanyIdAndStatusIdAndIsDeletedFalse(UUID companyId, String statusId);

    List<BankTransactionEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<BankTransactionEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
