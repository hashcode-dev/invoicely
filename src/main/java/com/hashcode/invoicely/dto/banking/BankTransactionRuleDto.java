package com.hashcode.invoicely.dto.banking;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryDto;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionRuleDto {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    private String name;

    @JsonProperty("applies_to")
    @Builder.Default
    private String appliesTo = "all";

    @Builder.Default
    private List<RuleCriteriaDto> rules = new ArrayList<>();

    @JsonProperty("auto_convert")
    @Builder.Default
    private boolean autoConvert = false;

    @JsonProperty("matches_on_all")
    @Builder.Default
    private boolean matchesOnAll = true;

    @JsonProperty("category_id")
    private UUID categoryId;

    @JsonProperty("vendor_id")
    private UUID vendorId;

    @JsonProperty("client_id")
    private UUID clientId;

    private VendorDto vendor;

    @JsonProperty("expense_category")
    private ExpenseCategoryDto expenseCategory;

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
