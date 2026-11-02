package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.SupportTicketMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessageEntity, UUID> {
    List<SupportTicketMessageEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
