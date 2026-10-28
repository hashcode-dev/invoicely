package com.hashcode.invoicely.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.client.ClientDto;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
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
public class PaymentDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("client_contact_id")
    private UUID clientContactId;

    @JsonProperty("company_gateway_id")
    private UUID companyGatewayId;

    private String number;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal refunded = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal applied = BigDecimal.ZERO;

    private String date;

    @JsonProperty("type_id")
    @Builder.Default
    private String typeId = "1";

    @JsonProperty("transaction_reference")
    private String transactionReference;

    @JsonProperty("private_notes")
    private String privateNotes;

    @JsonProperty("status_id")
    @Builder.Default
    private String statusId = "1";

    @JsonProperty("is_manual")
    @Builder.Default
    private boolean isManual = true;

    @JsonProperty("currency_id")
    @Builder.Default
    private String currencyId = "USD";

    @JsonProperty("exchange_rate")
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @JsonProperty("custom_value1")
    private String customValue1;

    @JsonProperty("custom_value2")
    private String customValue2;

    @JsonProperty("custom_value3")
    private String customValue3;

    @JsonProperty("custom_value4")
    private String customValue4;

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
    private List<PaymentableDto> paymentables = new ArrayList<>();

    @Builder.Default
    private List<InvoiceDto> invoices = new ArrayList<>();

    private ClientDto client;

    @Builder.Default
    private List<Object> documents = Collections.emptyList();
}
