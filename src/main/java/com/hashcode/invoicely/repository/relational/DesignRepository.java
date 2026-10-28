package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.DesignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DesignRepository extends JpaRepository<DesignEntity, UUID>, JpaSpecificationExecutor<DesignEntity> {

    Optional<DesignEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<DesignEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    List<DesignEntity> findByCompanyIdIsNullAndIsDeletedFalse();
}
