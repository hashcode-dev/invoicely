package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.InvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity, UUID> {

    List<InvoiceItemEntity> findByInvoiceId(UUID invoiceId);
}
