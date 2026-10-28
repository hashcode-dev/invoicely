package com.hashcode.invoicely.dto.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrExtractionResponse {

    private String id;

    @Builder.Default
    private String status = "COMPLETED";

    @JsonProperty("extracted_fields")
    private Map<String, Object> extractedFields;

    @Builder.Default
    private Double confidence = 0.98;

    @JsonProperty("created_at")
    private Long createdAt;
}
