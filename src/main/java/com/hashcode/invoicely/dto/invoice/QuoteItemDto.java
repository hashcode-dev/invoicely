package com.hashcode.invoicely.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteItemDto {

    private UUID id;

    @JsonProperty("product_key")
    private String productKey;

    private String notes;

    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @JsonProperty("is_amount_discount")
    @Builder.Default
    private boolean isAmountDiscount = false;

    @JsonProperty("line_total")
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @JsonProperty("tax_name1")
    private String taxName1;

    @JsonProperty("tax_rate1")
    @Builder.Default
    private BigDecimal taxRate1 = BigDecimal.ZERO;

    @JsonProperty("sort_id")
    @Builder.Default
    private Integer sortId = 0;
}
