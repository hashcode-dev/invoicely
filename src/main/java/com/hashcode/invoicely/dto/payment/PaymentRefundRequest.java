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
public class PaymentRefundRequest {

    private UUID id;

    private String date;

    private BigDecimal amount;

    @Builder.Default
    private List<PaymentInvoiceAllocationRequest> invoices = new ArrayList<>();
}
