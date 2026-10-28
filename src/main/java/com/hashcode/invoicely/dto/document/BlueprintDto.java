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
public class BlueprintDto {

    private UUID id;

    private String name;

    @Builder.Default
    private String status = "draft";

    private String content;

    @JsonProperty("is_template")
    @Builder.Default
    private boolean isTemplate = false;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("archived_at")
    private Long archivedAt;
}
