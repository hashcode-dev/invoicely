package com.hashcode.invoicely.dto.procurement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.client.ClientDto;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
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
public class ExpenseDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    @JsonProperty("vendor_id")
    private UUID vendorId;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    @JsonProperty("category_id")
    private UUID categoryId;

    @JsonProperty("purchase_order_id")
    private UUID purchaseOrderId;

    private String number;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @JsonProperty("foreign_amount")
    @Builder.Default
    private BigDecimal foreignAmount = BigDecimal.ZERO;

    @JsonProperty("exchange_rate")
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @JsonProperty("currency_id")
    @Builder.Default
    private String currencyId = "USD";

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

    @JsonProperty("tax_amount1")
    @Builder.Default
    private BigDecimal taxAmount1 = BigDecimal.ZERO;

    @JsonProperty("tax_amount2")
    @Builder.Default
    private BigDecimal taxAmount2 = BigDecimal.ZERO;

    @JsonProperty("tax_amount3")
    @Builder.Default
    private BigDecimal taxAmount3 = BigDecimal.ZERO;

    @JsonProperty("uses_inclusive_taxes")
    @Builder.Default
    private boolean usesInclusiveTaxes = false;

    @JsonProperty("should_be_invoiced")
    @Builder.Default
    private boolean shouldBeInvoiced = false;

    private String date;

    @JsonProperty("payment_date")
    private String paymentDate;

    @JsonProperty("payment_type_id")
    @Builder.Default
    private String paymentTypeId = "1";

    @JsonProperty("transaction_reference")
    private String transactionReference;

    @JsonProperty("public_notes")
    private String publicNotes;

    @JsonProperty("private_notes")
    private String privateNotes;

    @JsonProperty("custom_value1")
    private String customValue1;

    @JsonProperty("custom_value2")
    private String customValue2;

    @JsonProperty("custom_value3")
    private String customValue3;

    @JsonProperty("custom_value4")
    private String customValue4;

    @JsonProperty("status_id")
    @Builder.Default
    private String statusId = "logged";

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("archived_at")
    private Long archivedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    private VendorDto vendor;

    private ClientDto client;

    private ExpenseCategoryDto category;

    private InvoiceDto invoice;

    @Builder.Default
    private List<Object> documents = Collections.emptyList();
}
