package com.hashcode.invoicely.dto.procurement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    @JsonProperty("product_key")
    private String productKey;

    private String notes;

    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @JsonProperty("tax_name1")
    private String taxName1;

    @JsonProperty("tax_rate1")
    @Builder.Default
    private BigDecimal taxRate1 = BigDecimal.ZERO;

    @JsonProperty("tax_name2")
    private String taxName2;

    @JsonProperty("tax_rate2")
    @Builder.Default
    private BigDecimal taxRate2 = BigDecimal.ZERO;

    @JsonProperty("tax_name3")
    private String taxName3;

    @JsonProperty("tax_rate3")
    @Builder.Default
    private BigDecimal taxRate3 = BigDecimal.ZERO;

    @JsonProperty("custom_value1")
    private String customValue1;

    @JsonProperty("custom_value2")
    private String customValue2;

    @JsonProperty("custom_value3")
    private String customValue3;

    @JsonProperty("custom_value4")
    private String customValue4;

    @JsonProperty("in_stock_quantity")
    @Builder.Default
    private BigDecimal inStockQuantity = BigDecimal.ZERO;

    @JsonProperty("stock_notification")
    @Builder.Default
    private boolean stockNotification = false;

    @JsonProperty("stock_notification_threshold")
    @Builder.Default
    private BigDecimal stockNotificationThreshold = BigDecimal.ZERO;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("archived_at")
    private Long archivedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @Builder.Default
    private List<Object> documents = Collections.emptyList();
}
