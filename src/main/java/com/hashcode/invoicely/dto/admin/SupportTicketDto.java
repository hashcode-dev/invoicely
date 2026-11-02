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
public class SupportTicketDto {

    private UUID id;

    @JsonProperty("ticket_number")
    private String ticketNumber;

    @JsonProperty("account_id")
    private UUID accountId;

    @JsonProperty("company_id")
    private UUID companyId;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("reporter_email")
    private String reporterEmail;

    private String subject;
    private String status;
    private String priority;
    private String category;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("resolved_at")
    private Long resolvedAt;
}
