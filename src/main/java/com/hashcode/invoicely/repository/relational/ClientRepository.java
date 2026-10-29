package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID>, JpaSpecificationExecutor<ClientEntity> {

    Optional<ClientEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<ClientEntity> findByCompanyIdAndClientNumber(UUID companyId, String clientNumber);

    List<ClientEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<ClientEntity> findByCompanyId(UUID companyId, Pageable pageable);

    List<ClientEntity> findByCompanyIdAndIsDeletedFalse(UUID companyId);
}
