package com.hashcode.invoicely.dto.statics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaticsResponse {

    @Builder.Default
    private List<Map<String, Object>> currencies = Collections.emptyList();

    @Builder.Default
    private List<Map<String, Object>> countries = Collections.emptyList();

    @JsonProperty("date_formats")
    @Builder.Default
    private List<Map<String, Object>> dateFormats = Collections.emptyList();

    @JsonProperty("datetime_formats")
    @Builder.Default
    private List<Map<String, Object>> datetimeFormats = Collections.emptyList();

    @Builder.Default
    private List<Map<String, Object>> gateways = Collections.emptyList();

    @Builder.Default
    private List<Map<String, Object>> industries = Collections.emptyList();

    @Builder.Default
    private List<Map<String, Object>> languages = Collections.emptyList();

    @JsonProperty("payment_types")
    @Builder.Default
    private List<Map<String, Object>> paymentTypes = Collections.emptyList();

    @Builder.Default
    private List<Map<String, Object>> sizes = Collections.emptyList();

    @Builder.Default
    private List<Map<String, Object>> timezones = Collections.emptyList();

    @Builder.Default
    private Map<String, Object> templates = Collections.emptyMap();

    @Builder.Default
    private List<Map<String, Object>> banks = Collections.emptyList();

    @JsonProperty("bulk_updates")
    @Builder.Default
    private Map<String, List<String>> bulkUpdates = Collections.emptyMap();

    @JsonProperty("license_key")
    @Builder.Default
    private String licenseKey = "";
}
