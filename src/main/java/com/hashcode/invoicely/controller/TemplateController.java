package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.document.TemplateRequest;
import com.hashcode.invoicely.dto.document.TemplateResponse;
import com.hashcode.invoicely.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateResponse>> renderTemplate(@RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.renderTemplate(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping(value = "/preview/{hash}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> renderPreview(@PathVariable String hash) {
        byte[] pdfBytes = templateService.renderPreview(hash);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"template_preview.pdf\"")
                .body(pdfBytes);
    }
}
