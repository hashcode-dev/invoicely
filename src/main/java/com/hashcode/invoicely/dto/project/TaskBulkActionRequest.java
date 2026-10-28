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
public class TaskBulkActionRequest {

    private String action; // archive, restore, delete, invoice_tasks, invoice

    private List<UUID> ids;
}
