package com.hashcode.invoicely.repository.document;

import com.hashcode.invoicely.entity.document.InvoicelyOcrExtractionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoicelyOcrRepository extends MongoRepository<InvoicelyOcrExtractionDocument, String> {

    Optional<InvoicelyOcrExtractionDocument> findByCompanyIdAndId(String companyId, String id);

    List<InvoicelyOcrExtractionDocument> findByCompanyId(String companyId);
}
