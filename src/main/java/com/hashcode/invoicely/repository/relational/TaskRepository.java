package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID>, JpaSpecificationExecutor<TaskEntity> {

    Optional<TaskEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<TaskEntity> findByCompanyIdAndProjectIdAndIsDeletedFalse(UUID companyId, UUID projectId);

    List<TaskEntity> findByCompanyIdAndClientIdAndIsDeletedFalse(UUID companyId, UUID clientId);

    List<TaskEntity> findByCompanyIdAndStatusIdAndIsDeletedFalse(UUID companyId, UUID statusId);

    List<TaskEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<TaskEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
