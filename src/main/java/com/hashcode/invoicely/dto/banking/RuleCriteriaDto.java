package com.hashcode.invoicely.dto.banking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleCriteriaDto {

    @JsonProperty("search_key")
    private String searchKey; // description, amount, participant

    private String operator; // contains, equals, starts_with, greater_than, less_than

    private String value;
}
