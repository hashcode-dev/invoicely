package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.document.DocumentBulkActionRequest;
import com.hashcode.invoicely.dto.document.DocumentDto;
import com.hashcode.invoicely.dto.document.DocumentVisibilityRequest;
import com.hashcode.invoicely.service.DocumentService;
import com.hashcode.invoicely.service.PdfRenderingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final PdfRenderingService pdfRenderingService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<DocumentDto>> getDocuments(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "50") int perPage,
            @RequestParam(name = "status", defaultValue = "active") String status,
            @RequestParam(name = "company_documents", required = false) Boolean companyDocuments,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort) {
        return ResponseEntity.ok(documentService.getDocuments(page, perPage, status, companyDocuments, filter, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDto>> getDocumentById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(documentService.getDocumentById(id)));
    }

    @GetMapping(value = "/{hash}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String hash) {
        DocumentDto doc = documentService.getDocumentByHash(hash);
        byte[] pdfBytes = pdfRenderingService.generatePdf(doc.getName(), "Content for document: " + doc.getName(), Map.of("hash", hash));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getName() + "\"")
                .body(pdfBytes);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDto>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "client_id", required = false) UUID clientId,
            @RequestParam(name = "vendor_id", required = false) UUID vendorId,
            @RequestParam(name = "invoice_id", required = false) UUID invoiceId,
            @RequestParam(name = "expense_id", required = false) UUID expenseId) {
        DocumentDto uploaded = documentService.uploadDocument(file, clientId, vendorId, invoiceId, expenseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(uploaded));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDto>> updateVisibility(
            @PathVariable UUID id,
            @RequestBody DocumentVisibilityRequest request) {
        DocumentDto updated = documentService.updateVisibility(id, request);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> handleBulkAction(@RequestBody DocumentBulkActionRequest request) {
        int processed = documentService.handleBulkAction(request);
        return ResponseEntity.ok(Map.of("processed", processed));
    }
}
