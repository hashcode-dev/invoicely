package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.QuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<QuoteEntity, UUID>, JpaSpecificationExecutor<QuoteEntity> {

    Optional<QuoteEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<QuoteEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);
}
