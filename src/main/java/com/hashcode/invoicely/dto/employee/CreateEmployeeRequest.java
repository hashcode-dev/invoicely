package com.hashcode.invoicely.dto.employee;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hashcode.invoicely.dto.auth.CompanyUserDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String phone;

    private String password;

    @JsonProperty("is_admin")
    @Builder.Default
    private boolean isAdmin = false;

    @Builder.Default
    private Long permissions = 0L;

    @JsonProperty("company_user")
    private CompanyUserDto companyUser;

    public boolean resolveIsAdmin() {
        if (companyUser != null) {
            return companyUser.isAdmin();
        }
        return isAdmin;
    }

    public long resolvePermissions() {
        if (companyUser != null && companyUser.getPermissions() != null) {
            try {
                return Long.parseLong(companyUser.getPermissions());
            } catch (NumberFormatException e) {
                return -1L;
            }
        }
        return permissions != null ? permissions : 0L;
    }
}
