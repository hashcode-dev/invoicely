package com.hashcode.invoicely.dto.company;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCompanyRequest {

    @JsonProperty("company_name")
    private String companyName;

    private String subdomain;

    @JsonProperty("currency_id")
    @Builder.Default
    private String currencyId = "USD";

    @JsonProperty("country_id")
    @Builder.Default
    private String countryId = "US";

    @JsonProperty("size_id")
    private String sizeId;

    @JsonProperty("industry_id")
    private String industryId;

    @JsonProperty("vat_number")
    private String vatNumber;

    @JsonProperty("gst_number")
    private String gstNumber;
}
