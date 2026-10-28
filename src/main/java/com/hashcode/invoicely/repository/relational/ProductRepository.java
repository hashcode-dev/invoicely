package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<ProductEntity> findByCompanyIdAndProductKey(UUID companyId, String productKey);

    List<ProductEntity> findByCompanyIdAndIdIn(UUID companyId, List<UUID> ids);

    Page<ProductEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
