package com.hashcode.invoicely.dto.procurement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    @JsonProperty("vendor_id")
    private UUID vendorId;

    @JsonProperty("client_id")
    private UUID clientId;

    private String number;

    @JsonProperty("po_number")
    private String poNumber;

    private String date;

    @JsonProperty("due_date")
    private String dueDate;

    @JsonProperty("status_id")
    @Builder.Default
    private String statusId = "draft";

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonProperty("paid_to_date")
    @Builder.Default
    private BigDecimal paidToDate = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @JsonProperty("is_amount_discount")
    @Builder.Default
    private boolean isAmountDiscount = false;

    @JsonProperty("total_taxes")
    @Builder.Default
    private BigDecimal totalTaxes = BigDecimal.ZERO;

    private String terms;

    @JsonProperty("public_notes")
    private String publicNotes;

    @JsonProperty("private_notes")
    private String privateNotes;

    private String footer;

    @JsonProperty("uses_inclusive_taxes")
    @Builder.Default
    private boolean usesInclusiveTaxes = false;

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

    @JsonProperty("exchange_rate")
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @JsonProperty("design_id")
    @Builder.Default
    private String designId = "clean";

    @JsonProperty("expense_id")
    private UUID expenseId;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("archived_at")
    private Long archivedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("line_items")
    @Builder.Default
    private List<PurchaseOrderItemDto> lineItems = new ArrayList<>();

    private VendorDto vendor;

    private ExpenseDto expense;

    @Builder.Default
    private List<Object> documents = Collections.emptyList();
}
