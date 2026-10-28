package com.hashcode.invoicely.dto.client;

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
public class ClientBulkActionRequest {

    private String action; // archive, restore, delete, clone

    @Builder.Default
    private List<UUID> ids = new ArrayList<>();

    @JsonProperty("group_settings_id")
    private String groupSettingsId;

    private String column;

    @JsonProperty("new_value")
    private Object newValue;
}
