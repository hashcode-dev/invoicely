package com.hashcode.invoicely.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBulkActionRequest {

    private String action; // archive, restore, delete

    private List<UUID> ids;
}
