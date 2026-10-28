package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyUserRepository extends JpaRepository<CompanyUserEntity, UUID> {
    List<CompanyUserEntity> findByUserId(UUID userId);
    List<CompanyUserEntity> findByCompanyId(UUID companyId);
    Optional<CompanyUserEntity> findByCompanyIdAndUserId(UUID companyId, UUID userId);
}
