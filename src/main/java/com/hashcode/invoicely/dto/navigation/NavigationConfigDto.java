package com.hashcode.invoicely.dto.navigation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavigationConfigDto {
    @JsonProperty("item_key")
    private String itemKey;

    @JsonProperty("is_visible")
    @Builder.Default
    private boolean isVisible = true;

    @JsonProperty("display_order")
    @Builder.Default
    private int displayOrder = 0;

    @JsonProperty("custom_label")
    private String customLabel;

    @JsonProperty("is_locked")
    @Builder.Default
    private boolean isLocked = false;

    private String role;
}
