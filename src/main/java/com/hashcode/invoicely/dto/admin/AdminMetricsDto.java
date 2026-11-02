package com.hashcode.invoicely.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMetricsDto {

    @JsonProperty("total_tenants")
    private long totalTenants;

    @JsonProperty("total_active_users")
    private long totalActiveUsers;

    @JsonProperty("total_invoices")
    private long totalInvoices;

    @JsonProperty("total_invoiced_amount")
    @Builder.Default
    private BigDecimal totalInvoicedAmount = BigDecimal.ZERO;

    @JsonProperty("total_revenue_collected")
    @Builder.Default
    private BigDecimal totalRevenueCollected = BigDecimal.ZERO;

    @JsonProperty("open_tickets_count")
    private long openTicketsCount;
}
