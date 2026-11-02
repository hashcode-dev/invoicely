package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.TenantNavigationConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantNavigationConfigRepository extends JpaRepository<TenantNavigationConfigEntity, UUID> {
    List<TenantNavigationConfigEntity> findByCompanyIdOrderByDisplayOrderAsc(UUID companyId);
    List<TenantNavigationConfigEntity> findByCompanyIdAndRoleOrderByDisplayOrderAsc(UUID companyId, String role);
    Optional<TenantNavigationConfigEntity> findByCompanyIdAndRoleAndItemKey(UUID companyId, String role, String itemKey);
}
