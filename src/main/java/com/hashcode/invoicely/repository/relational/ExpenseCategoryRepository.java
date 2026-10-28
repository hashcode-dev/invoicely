package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.ExpenseCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategoryEntity, UUID>, JpaSpecificationExecutor<ExpenseCategoryEntity> {

    Optional<ExpenseCategoryEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<ExpenseCategoryEntity> findByCompanyIdAndName(UUID companyId, String name);

    List<ExpenseCategoryEntity> findByCompanyIdAndIsDeletedFalse(UUID companyId);

    List<ExpenseCategoryEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<ExpenseCategoryEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
