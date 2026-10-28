package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID>, JpaSpecificationExecutor<ProjectEntity> {

    Optional<ProjectEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<ProjectEntity> findByCompanyIdAndName(UUID companyId, String name);

    List<ProjectEntity> findByCompanyIdAndIsDeletedFalse(UUID companyId);

    List<ProjectEntity> findByCompanyIdAndClientIdAndIsDeletedFalse(UUID companyId, UUID clientId);

    List<ProjectEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<ProjectEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
