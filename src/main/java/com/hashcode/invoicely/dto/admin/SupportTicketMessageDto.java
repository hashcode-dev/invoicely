package com.hashcode.invoicely.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketMessageDto {

    private UUID id;

    @JsonProperty("ticket_id")
    private UUID ticketId;

    @JsonProperty("sender_user_id")
    private UUID senderUserId;

    @JsonProperty("sender_email")
    private String senderEmail;

    @JsonProperty("is_internal_note")
    private boolean isInternalNote;

    @JsonProperty("message_body")
    private String messageBody;

    @JsonProperty("created_at")
    private Long createdAt;
}
