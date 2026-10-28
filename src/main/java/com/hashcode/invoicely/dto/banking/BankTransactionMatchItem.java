package com.hashcode.invoicely.dto.banking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionMatchItem {

    private UUID id;

    @JsonProperty("invoice_ids")
    private String invoiceIds;

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("expense_id")
    private String expenseId;

    @JsonProperty("vendor_id")
    private String vendorId;

    @JsonProperty("category_id")
    private String categoryId;

    @JsonProperty("invoicely_category_id")
    @com.fasterxml.jackson.annotation.JsonAlias({"ninja_category_id", "invoicely_category_id"})
    private String invoicelyCategoryId;
}
