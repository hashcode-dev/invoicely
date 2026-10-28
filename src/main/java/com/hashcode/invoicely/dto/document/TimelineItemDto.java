package com.hashcode.invoicely.dto.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineItemDto {

    private String id;

    private String title;

    private String description;

    private String type;

    @JsonProperty("created_at")
    private Long createdAt;
}
