package com.hashcode.invoicely.dto.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableColumnConfigDto {

    private UUID id;

    @JsonProperty("company_id")
    private UUID companyId;

    @JsonProperty("table_name")
    private String tableName;

    @JsonProperty("default_columns")
    @Builder.Default
    private List<String> defaultColumns = Collections.emptyList();

    @JsonProperty("locked_columns")
    @Builder.Default
    private List<String> lockedColumns = Collections.emptyList();

    @JsonProperty("restricted_for_employees")
    @Builder.Default
    private List<String> restrictedForEmployees = Collections.emptyList();

    @JsonProperty("allow_employee_customization")
    @Builder.Default
    private boolean allowEmployeeCustomization = true;
}
