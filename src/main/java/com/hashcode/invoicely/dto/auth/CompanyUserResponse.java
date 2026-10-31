package com.hashcode.invoicely.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUserResponse {

    private UserDto user;
    private CompanyDto company;
    private AccountDto account;
    private TokenDto token;

    private String permissions;

    @JsonProperty("is_owner")
    @Builder.Default
    private boolean isOwner = false;

    @JsonProperty("is_admin")
    @Builder.Default
    private boolean isAdmin = false;

    @JsonProperty("is_locked")
    @Builder.Default
    private boolean isLocked = false;

    @JsonProperty("is_employee")
    @Builder.Default
    private boolean isEmployee = false;

    private String role;

    @Builder.Default
    private Map<String, Object> notifications = Map.of("email", Collections.emptyList());

    @Builder.Default
    private Map<String, Object> settings = Map.of("accent_color", "#004bca");

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
