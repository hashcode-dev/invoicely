package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.PurchaseOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItemEntity, UUID> {

    List<PurchaseOrderItemEntity> findByPurchaseOrderId(UUID purchaseOrderId);
}
