package com.hashcode.invoicely.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    @JsonProperty("company_gateway_id")
    private UUID companyGatewayId;

    private String number;

    private BigDecimal amount;

    private String date;

    @JsonProperty("type_id")
    private String typeId;

    @JsonProperty("transaction_reference")
    private String transactionReference;

    @JsonProperty("private_notes")
    private String privateNotes;

    @JsonProperty("status_id")
    private String statusId;

    @JsonProperty("is_manual")
    @Builder.Default
    private boolean isManual = true;

    @JsonProperty("custom_value1")
    private String customValue1;

    @JsonProperty("custom_value2")
    private String customValue2;

    @JsonProperty("custom_value3")
    private String customValue3;

    @JsonProperty("custom_value4")
    private String customValue4;

    @Builder.Default
    private List<PaymentInvoiceAllocationRequest> invoices = new ArrayList<>();

    @Builder.Default
    private List<PaymentInvoiceAllocationRequest> paymentables = new ArrayList<>();
}
