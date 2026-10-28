package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.BankTransactionRuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankTransactionRuleRepository extends JpaRepository<BankTransactionRuleEntity, UUID>, JpaSpecificationExecutor<BankTransactionRuleEntity> {

    Optional<BankTransactionRuleEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<BankTransactionRuleEntity> findByCompanyIdAndIsDeletedFalse(UUID companyId);

    List<BankTransactionRuleEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<BankTransactionRuleEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
