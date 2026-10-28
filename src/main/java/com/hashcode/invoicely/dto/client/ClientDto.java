package com.hashcode.invoicely.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("assigned_user_id")
    private UUID assignedUserId;

    private String name;

    @JsonProperty("display_name")
    private String displayName;

    private String number;

    private String website;

    private String phone;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonProperty("paid_to_date")
    @Builder.Default
    private BigDecimal paidToDate = BigDecimal.ZERO;

    @JsonProperty("credit_balance")
    @Builder.Default
    private BigDecimal creditBalance = BigDecimal.ZERO;

    @JsonProperty("currency_id")
    @Builder.Default
    private String currencyId = "USD";

    private String address1;

    private String address2;

    private String city;

    private String state;

    @JsonProperty("postal_code")
    private String postalCode;

    @JsonProperty("country_id")
    private String countryId;

    @JsonProperty("shipping_address1")
    private String shippingAddress1;

    @JsonProperty("shipping_address2")
    private String shippingAddress2;

    @JsonProperty("shipping_city")
    private String shippingCity;

    @JsonProperty("shipping_state")
    private String shippingState;

    @JsonProperty("shipping_postal_code")
    private String shippingPostalCode;

    @JsonProperty("shipping_country_id")
    private String shippingCountryId;

    @JsonProperty("vat_number")
    private String vatNumber;

    @JsonProperty("id_number")
    private String idNumber;

    @JsonProperty("routing_id")
    private String routingId;

    @JsonProperty("public_notes")
    private String publicNotes;

    @JsonProperty("private_notes")
    private String privateNotes;

    @JsonProperty("custom_value1")
    private String customValue1;

    @JsonProperty("custom_value2")
    private String customValue2;

    @JsonProperty("custom_value3")
    private String customValue3;

    @JsonProperty("custom_value4")
    private String customValue4;

    @JsonProperty("is_tax_exempt")
    @Builder.Default
    private boolean isTaxExempt = false;

    @JsonProperty("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @JsonProperty("archived_at")
    private Long archivedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @Builder.Default
    private List<ClientContactDto> contacts = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> settings = Collections.emptyMap();

    @JsonProperty("gateway_tokens")
    @Builder.Default
    private List<Object> gatewayTokens = Collections.emptyList();

    @Builder.Default
    private List<Object> documents = Collections.emptyList();
}
