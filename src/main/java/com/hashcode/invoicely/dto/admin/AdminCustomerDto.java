package com.hashcode.invoicely.dto.admin;

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
public class AdminCustomerDto {

    @JsonProperty("company_id")
    private UUID companyId;

    @JsonProperty("account_id")
    private UUID accountId;

    @JsonProperty("company_name")
    private String companyName;

    private String subdomain;
    private String plan;

    @JsonProperty("owner_email")
    private String ownerEmail;

    @JsonProperty("users_count")
    private long usersCount;

    @JsonProperty("invoices_count")
    private long invoicesCount;

    @JsonProperty("total_invoiced")
    @Builder.Default
    private BigDecimal totalInvoiced = BigDecimal.ZERO;

    @JsonProperty("total_paid")
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @JsonProperty("created_at")
    private Long createdAt;
}
