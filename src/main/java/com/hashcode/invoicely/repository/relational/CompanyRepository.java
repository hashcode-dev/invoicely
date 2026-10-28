package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID> {
    Optional<CompanyEntity> findBySubdomainAndIsDeletedFalse(String subdomain);
    List<CompanyEntity> findByAccountIdAndIsDeletedFalse(UUID accountId);
}
