package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.document.BlueprintBulkActionRequest;
import com.hashcode.invoicely.dto.document.BlueprintDto;
import com.hashcode.invoicely.dto.document.OcrExtractionResponse;
import com.hashcode.invoicely.dto.document.TimelineItemDto;
import com.hashcode.invoicely.entity.relational.BlueprintEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface InvoicelyDocsService {

    OcrExtractionResponse extractDocument(MultipartFile file);

    List<TimelineItemDto> getDocumentTimeline(UUID documentId);

    GenericManyResponse<BlueprintDto> getBlueprints(int page, int perPage, String status, String filter, String sort);

    BlueprintDto getBlueprintById(UUID id);

    BlueprintDto createBlueprint(BlueprintDto dto);

    BlueprintDto sendBlueprint(UUID id);

    int handleBulkAction(BlueprintBulkActionRequest request);

    BlueprintDto toDto(BlueprintEntity entity);
}
