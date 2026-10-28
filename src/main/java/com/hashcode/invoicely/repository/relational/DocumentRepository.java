package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID>, JpaSpecificationExecutor<DocumentEntity> {

    Optional<DocumentEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<DocumentEntity> findByCompanyIdAndHash(UUID companyId, String hash);

    Optional<DocumentEntity> findByHash(String hash);

    List<DocumentEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);
}
