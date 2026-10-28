package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.entity.relational.QuoteItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuoteItemRepository extends JpaRepository<QuoteItemEntity, UUID> {

    List<QuoteItemEntity> findByQuoteId(UUID quoteId);
}
