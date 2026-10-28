package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.ApiTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiTokenEntity, UUID> {
    Optional<ApiTokenEntity> findByTokenHashAndIsDeletedFalse(String tokenHash);
    List<ApiTokenEntity> findByCompanyIdAndIsDeletedFalse(UUID companyId);
    List<ApiTokenEntity> findByUserIdAndIsDeletedFalse(UUID userId);
}
