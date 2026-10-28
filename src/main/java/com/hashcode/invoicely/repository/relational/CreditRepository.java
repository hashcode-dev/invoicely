package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.CreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditRepository extends JpaRepository<CreditEntity, UUID>, JpaSpecificationExecutor<CreditEntity> {

    Optional<CreditEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<CreditEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);
}
