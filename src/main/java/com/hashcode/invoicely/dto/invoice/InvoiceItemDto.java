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
public class InvoiceItemDto {

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

    @JsonProperty("gross_line_total")
    @Builder.Default
    private BigDecimal grossLineTotal = BigDecimal.ZERO;

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

    @JsonProperty("type_id")
    @Builder.Default
    private String typeId = "1";

    @JsonProperty("sort_id")
    @Builder.Default
    private Integer sortId = 0;

    @JsonProperty("custom_value1")
    private String customValue1;

    @JsonProperty("custom_value2")
    private String customValue2;

    @JsonProperty("custom_value3")
    private String customValue3;

    @JsonProperty("custom_value4")
    private String customValue4;
}
