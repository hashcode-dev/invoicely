package com.hashcode.invoicely.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDto {
    private UUID id;

    @JsonProperty("company_name")
    private String companyName;

    private String subdomain;

    @JsonProperty("currency_id")
    private String currencyId;

    @JsonProperty("country_id")
    private String countryId;

    @JsonProperty("size_id")
    private String sizeId;

    @JsonProperty("industry_id")
    private String industryId;

    @JsonProperty("vat_number")
    private String vatNumber;

    @JsonProperty("gst_number")
    private String gstNumber;

    @JsonProperty("portal_domain")
    private String portalDomain;

    @Builder.Default
    private Map<String, Object> settings = Collections.emptyMap();

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
