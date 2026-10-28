package com.hashcode.invoicely.dto.banking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionDto {

    private UUID id;

    @JsonProperty("transaction_id")
    private UUID transactionId; // Alias for id used in front-desk

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("bank_integration_id")
    private UUID bankIntegrationId;

    @JsonProperty("bank_transaction_rule_id")
    private UUID bankTransactionRuleId;

    private LocalDate date;

    private BigDecimal amount;

    @JsonProperty("currency_id")
    @Builder.Default
    private String currencyId = "USD";

    private String description;

    @JsonProperty("base_type")
    @Builder.Default
    private String baseType = "debit";

    @JsonProperty("status_id")
    @Builder.Default
    private String statusId = "unmatched";

    @JsonProperty("payment_id")
    private UUID paymentId;

    @JsonProperty("expense_id")
    private UUID expenseId;

    @JsonProperty("vendor_id")
    private UUID vendorId;

    @JsonProperty("category_id")
    private UUID categoryId;

    @JsonProperty("invoicely_category_id")
    private UUID invoicelyCategoryId;

    private String participant;

    @JsonProperty("participant_name")
    private String participantName;

    @JsonProperty("invoice_ids")
    private String invoiceIds;

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("category_type")
    private String categoryType;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("archived_at")
    private Long archivedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
