package com.hashcode.invoicely.dto.vendor;

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
public class VendorBulkActionRequest {

    private String action; // archive, restore, delete

    @Builder.Default
    private List<UUID> ids = new ArrayList<>();
}
