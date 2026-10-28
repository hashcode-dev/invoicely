package com.hashcode.invoicely.entity.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "invoicely_ocr_extractions")
public class InvoicelyOcrExtractionDocument {

    @Id
    private String id;

    @Indexed
    @Field("company_id")
    private String companyId;

    @Field("file_id")
    private String fileId;

    @Field("mime_type")
    private String mimeType;

    @Field("status")
    @Builder.Default
    private String status = "COMPLETED";

    @Field("extracted_data")
    private Map<String, Object> extractedData;

    @Field("confidence_scores")
    private Map<String, Object> confidenceScores;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
