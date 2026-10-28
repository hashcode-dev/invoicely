package com.hashcode.invoicely.dto.invoice;

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
public class QuoteBulkActionRequest {

    private String action; // archive, restore, delete, approve

    @Builder.Default
    private List<UUID> ids = new ArrayList<>();
}
