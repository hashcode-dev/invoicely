package com.hashcode.invoicely.dto.payment;

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
public class PaymentInvoiceAllocationRequest {

    private UUID id;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;
}
