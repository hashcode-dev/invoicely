package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.TableColumnConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableColumnConfigRepository extends JpaRepository<TableColumnConfigEntity, UUID> {
    List<TableColumnConfigEntity> findByCompanyId(UUID companyId);
    Optional<TableColumnConfigEntity> findByCompanyIdAndTableName(UUID companyId, String tableName);
}
