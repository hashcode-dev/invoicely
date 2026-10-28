package com.hashcode.invoicely.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.client.ClientDto;
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
public class QuoteDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    private String number;

    @JsonProperty("po_number")
    private String poNumber;

    private String date;

    @JsonProperty("due_date")
    private String dueDate;

    @JsonProperty("status_id")
    @Builder.Default
    private String statusId = "draft"; // draft, sent, approved, converted

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

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

    @JsonProperty("custom_value1")
    private String customValue1;

    @JsonProperty("custom_value2")
    private String customValue2;

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
    private List<QuoteItemDto> lineItems = new ArrayList<>();

    private ClientDto client;

    @Builder.Default
    private List<Object> invitations = Collections.emptyList();

    @Builder.Default
    private List<Object> documents = Collections.emptyList();
}
