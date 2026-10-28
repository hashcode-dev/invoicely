package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.CompanyGatewayEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyGatewayRepository extends JpaRepository<CompanyGatewayEntity, UUID>, JpaSpecificationExecutor<CompanyGatewayEntity> {

    Optional<CompanyGatewayEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<CompanyGatewayEntity> findByCompanyIdAndIsDeletedFalse(UUID companyId);

    List<CompanyGatewayEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<CompanyGatewayEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
