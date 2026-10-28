package com.hashcode.invoicely.dto.invoice;

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
public class RecurringInvoiceDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("client_id")
    private UUID clientId;

    private String number;

    @JsonProperty("frequency_id")
    @Builder.Default
    private String frequencyId = "monthly";

    @JsonProperty("status_id")
    @Builder.Default
    private String statusId = "active";

    @JsonProperty("auto_bill")
    @Builder.Default
    private boolean autoBill = false;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @JsonProperty("remaining_cycles")
    @Builder.Default
    private Integer remainingCycles = -1;

    @JsonProperty("next_send_date")
    private String nextSendDate;

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
