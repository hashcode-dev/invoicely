package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.document.DocumentBulkActionRequest;
import com.hashcode.invoicely.dto.document.DocumentDto;
import com.hashcode.invoicely.dto.document.DocumentVisibilityRequest;
import com.hashcode.invoicely.entity.relational.DocumentEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentService {

    GenericManyResponse<DocumentDto> getDocuments(int page, int perPage, String status, Boolean companyDocuments, String filter, String sort);

    DocumentDto getDocumentById(UUID id);

    DocumentDto getDocumentByHash(String hash);

    DocumentDto uploadDocument(MultipartFile file, UUID clientId, UUID vendorId, UUID invoiceId, UUID expenseId);

    DocumentDto updateVisibility(UUID id, DocumentVisibilityRequest request);

    void deleteDocument(UUID id);

    int handleBulkAction(DocumentBulkActionRequest request);

    DocumentDto toDto(DocumentEntity entity);
}
