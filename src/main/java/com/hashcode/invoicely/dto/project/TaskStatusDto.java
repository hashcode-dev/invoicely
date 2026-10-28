package com.hashcode.invoicely.dto.project;

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
public class TaskStatusDto {

    private UUID id;

    private String name;

    private String color;

    @JsonProperty("sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @JsonProperty("status_order")
    @Builder.Default
    private Integer statusOrder = 0;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("archived_at")
    private Long archivedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
