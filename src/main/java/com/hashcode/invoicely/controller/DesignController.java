package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.document.DesignBulkActionRequest;
import com.hashcode.invoicely.dto.document.DesignDto;
import com.hashcode.invoicely.dto.document.SetDefaultDesignRequest;
import com.hashcode.invoicely.service.DesignService;
import com.hashcode.invoicely.service.PdfRenderingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;
    private final PdfRenderingService pdfRenderingService;

    @GetMapping("/api/v1/designs")
    public ResponseEntity<GenericManyResponse<DesignDto>> getDesigns(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "50") int perPage,
            @RequestParam(name = "status", defaultValue = "active") String status,
            @RequestParam(name = "custom", required = false) Boolean custom,
            @RequestParam(name = "is_template", required = false) Boolean isTemplate,
            @RequestParam(name = "template", required = false) Boolean template,
            @RequestParam(name = "entities", required = false) String entities,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort) {
        Boolean templateFlag = isTemplate != null ? isTemplate : template;
        return ResponseEntity.ok(designService.getDesigns(page, perPage, status, custom, templateFlag, entities, filter, sort));
    }

    @GetMapping("/api/v1/designs/create")
    public ResponseEntity<ApiResponse<DesignDto>> getBlankDesign() {
        return ResponseEntity.ok(ApiResponse.of(designService.getBlankDesign()));
    }

    @GetMapping("/api/v1/designs/{id}")
    public ResponseEntity<ApiResponse<DesignDto>> getDesignById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(designService.getDesignById(id)));
    }

    @PostMapping("/api/v1/designs")
    public ResponseEntity<ApiResponse<DesignDto>> createDesign(@RequestBody DesignDto dto) {
        DesignDto created = designService.createDesign(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PutMapping("/api/v1/designs/{id}")
    public ResponseEntity<ApiResponse<DesignDto>> updateDesign(@PathVariable UUID id, @RequestBody DesignDto dto) {
        DesignDto updated = designService.updateDesign(id, dto);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @DeleteMapping("/api/v1/designs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDesign(@PathVariable UUID id) {
        designService.deleteDesign(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/api/v1/designs/bulk")
    public ResponseEntity<Map<String, Object>> handleBulkAction(@RequestBody DesignBulkActionRequest request) {
        int processed = designService.handleBulkAction(request);
        return ResponseEntity.ok(Map.of("processed", processed));
    }

    @PostMapping("/api/v1/designs/set/default")
    public ResponseEntity<Map<String, Object>> setDefaultDesign(@RequestBody SetDefaultDesignRequest request) {
        boolean success = designService.setDefaultDesign(request);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping(value = "/api/v1/live_design", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> renderLiveDesign(@RequestBody(required = false) Map<String, Object> payload) {
        byte[] pdfBytes = pdfRenderingService.generatePdf("Live Invoice Preview", "Preview rendered dynamically by Invoicely Design Engine.", payload);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"live_preview.pdf\"")
                .body(pdfBytes);
    }
}
