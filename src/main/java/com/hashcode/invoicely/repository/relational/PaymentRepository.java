package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID>, JpaSpecificationExecutor<PaymentEntity> {

    Optional<PaymentEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<PaymentEntity> findByCompanyIdAndNumber(UUID companyId, String number);

    List<PaymentEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<PaymentEntity> findByCompanyIdAndClientId(UUID companyId, UUID clientId, Pageable pageable);
}
