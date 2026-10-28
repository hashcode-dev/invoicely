package com.hashcode.invoicely.dto.procurement;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ExpenseBulkActionRequest {

    private String action; // archive, restore, delete, bulk_categorize, convert_to_invoice

    @Builder.Default
    private List<UUID> ids = new ArrayList<>();

    @JsonProperty("category_id")
    private UUID categoryId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    @JsonProperty("client_id")
    private UUID clientId;
}
