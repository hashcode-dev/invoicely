package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID>, JpaSpecificationExecutor<InvoiceEntity> {

    Optional<InvoiceEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<InvoiceEntity> findByCompanyIdAndNumber(UUID companyId, String number);

    List<InvoiceEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<InvoiceEntity> findByCompanyIdAndClientId(UUID companyId, UUID clientId, Pageable pageable);
}
