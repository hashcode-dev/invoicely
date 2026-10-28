package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.document.BlueprintBulkActionRequest;
import com.hashcode.invoicely.dto.document.BlueprintDto;
import com.hashcode.invoicely.dto.document.OcrExtractionResponse;
import com.hashcode.invoicely.dto.document.TimelineItemDto;
import com.hashcode.invoicely.service.InvoicelyDocsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InvoicelyDocsController {

    private final InvoicelyDocsService docsService;

    @PostMapping(
            value = {
                    "/api/client/account_management/v2/invoicely_docs/beta",
                    "/api/documents/ocr"
            },
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<OcrExtractionResponse> extractDocumentOcr(
            @RequestParam(name = "document", required = false) MultipartFile document,
            @RequestParam(name = "file", required = false) MultipartFile file) {
        MultipartFile upload = document != null ? document : file;
        OcrExtractionResponse response = docsService.extractDocument(upload);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/documents/{id}/timeline")
    public ResponseEntity<List<TimelineItemDto>> getDocumentTimeline(@PathVariable UUID id) {
        return ResponseEntity.ok(docsService.getDocumentTimeline(id));
    }

    @GetMapping("/api/blueprints")
    public ResponseEntity<GenericManyResponse<BlueprintDto>> getBlueprints(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "50") int perPage,
            @RequestParam(name = "status", defaultValue = "active") String status,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort) {
        return ResponseEntity.ok(docsService.getBlueprints(page, perPage, status, filter, sort));
    }

    @GetMapping("/api/blueprints/{id}")
    public ResponseEntity<ApiResponse<BlueprintDto>> getBlueprintById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(docsService.getBlueprintById(id)));
    }

    @PostMapping("/api/blueprints")
    public ResponseEntity<ApiResponse<BlueprintDto>> createBlueprint(@RequestBody BlueprintDto dto) {
        BlueprintDto created = docsService.createBlueprint(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PostMapping("/api/blueprints/{id}/send")
    public ResponseEntity<ApiResponse<BlueprintDto>> sendBlueprint(@PathVariable UUID id) {
        BlueprintDto sent = docsService.sendBlueprint(id);
        return ResponseEntity.ok(ApiResponse.of(sent));
    }

    @PostMapping("/api/blueprints/bulk")
    public ResponseEntity<Map<String, Object>> handleBulkAction(@RequestBody BlueprintBulkActionRequest request) {
        int processed = docsService.handleBulkAction(request);
        return ResponseEntity.ok(Map.of("processed", processed));
    }
}
