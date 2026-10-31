package com.hashcode.invoicely.dto.auth;

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
public class EmployeeLoginRequest {

    @NotBlank(message = "Organization ID is required")
    @JsonProperty("organization_id")
    private String organizationId;

    @NotBlank(message = "Employee ID or email is required")
    @JsonProperty("employee_id")
    private String employeeId;

    @NotBlank(message = "Password is required")
    private String password;
}
