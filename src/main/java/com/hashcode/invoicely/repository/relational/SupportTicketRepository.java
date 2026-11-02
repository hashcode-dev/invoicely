package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.SupportTicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, UUID> {
    Optional<SupportTicketEntity> findByTicketNumber(String ticketNumber);
    List<SupportTicketEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    Page<SupportTicketEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    long countByStatus(String status);
}
