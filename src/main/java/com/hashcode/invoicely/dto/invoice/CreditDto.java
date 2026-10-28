package com.hashcode.invoicely.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.client.ClientDto;
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
public class CreditDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    private String number;

    private String date;

    @JsonProperty("due_date")
    private String dueDate;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonProperty("status_id")
    @Builder.Default
    private String statusId = "draft";

    @JsonProperty("public_notes")
    private String publicNotes;

    @JsonProperty("private_notes")
    private String privateNotes;

    private String terms;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("archived_at")
    private Long archivedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    private ClientDto client;
}
