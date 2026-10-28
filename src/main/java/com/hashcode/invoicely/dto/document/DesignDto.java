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
public class DesignDto {

    private UUID id;

    private String name;

    private PartsDto design;

    @JsonProperty("is_custom")
    @Builder.Default
    private boolean isCustom = true;

    @JsonProperty("is_active")
    @Builder.Default
    private boolean isActive = true;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("is_free")
    @Builder.Default
    private boolean isFree = true;

    @JsonProperty("is_template")
    @Builder.Default
    private boolean isTemplate = false;

    @Builder.Default
    private String entities = "invoice";

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("archived_at")
    private Long archivedAt;
}
