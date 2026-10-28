package com.hashcode.invoicely.dto.banking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("bank_account_name")
    private String bankAccountName;

    @JsonProperty("bank_account_number")
    private String bankAccountNumber;

    @JsonProperty("bank_account_type")
    @Builder.Default
    private String bankAccountType = "checking";

    @JsonProperty("bank_account_status")
    @Builder.Default
    private String bankAccountStatus = "active";

    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    private String nickname;

    @JsonProperty("provider_name")
    @Builder.Default
    private String providerName = "manual";

    @JsonProperty("integration_type")
    @Builder.Default
    private String integrationType = "plaid";

    @JsonProperty("nordigen_institution_id")
    private String nordigenInstitutionId;

    @JsonProperty("auto_sync")
    @Builder.Default
    private boolean autoSync = true;

    @JsonProperty("disabled_upstream")
    @Builder.Default
    private boolean disabledUpstream = false;

    @JsonProperty("from_date")
    private LocalDate fromDate;

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
