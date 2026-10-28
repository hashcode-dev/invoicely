package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.PaymentableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentableRepository extends JpaRepository<PaymentableEntity, UUID> {

    List<PaymentableEntity> findByPaymentId(UUID paymentId);

    List<PaymentableEntity> findByInvoiceId(UUID invoiceId);
}
