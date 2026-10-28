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
public class PaymentableDto {

    private UUID id;

    @JsonProperty("payment_id")
    private UUID paymentId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal refunded = BigDecimal.ZERO;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
