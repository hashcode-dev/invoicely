package com.hashcode.invoicely.dto.procurement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderBulkActionRequest {

    private String action; // archive, restore, delete, mark_sent, expense

    @Builder.Default
    private List<UUID> ids = new ArrayList<>();
}
