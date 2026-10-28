package com.hashcode.invoicely.dto.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSortRequest {

    @JsonProperty("status_ids")
    private List<UUID> statusIds;

    @JsonProperty("task_ids")
    private Map<String, List<UUID>> taskIds;
}
