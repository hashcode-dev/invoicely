package com.hashcode.invoicely.dto.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDto {

    private UUID id;

    private String name;

    private String type;

    private Long size;

    private Integer width;

    private Integer height;

    private String hash;

    private String disk;

    private String url;

    private String preview;

    @JsonProperty("is_default")
    @Builder.Default
    private boolean isDefault = false;

    @JsonProperty("is_public")
    @Builder.Default
    private boolean isPublic = false;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("vendor_id")
    private UUID vendorId;

    @JsonProperty("project_id")
    private UUID projectId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    @JsonProperty("expense_id")
    private UUID expenseId;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("archived_at")
    private Long archivedAt;
}
