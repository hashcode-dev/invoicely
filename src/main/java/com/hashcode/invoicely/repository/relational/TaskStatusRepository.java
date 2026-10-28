package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.TaskStatusEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskStatusRepository extends JpaRepository<TaskStatusEntity, UUID>, JpaSpecificationExecutor<TaskStatusEntity> {

    Optional<TaskStatusEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<TaskStatusEntity> findByCompanyIdAndName(UUID companyId, String name);

    List<TaskStatusEntity> findByCompanyIdAndIsDeletedFalseOrderBySortOrderAsc(UUID companyId);

    List<TaskStatusEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<TaskStatusEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
