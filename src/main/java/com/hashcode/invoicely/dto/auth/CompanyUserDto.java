package com.hashcode.invoicely.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUserDto {

    private UUID id;

    @JsonProperty("company_id")
    private UUID companyId;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("account_id")
    private UUID accountId;

    @JsonProperty("is_owner")
    @Builder.Default
    private boolean isOwner = false;

    @JsonProperty("is_admin")
    @Builder.Default
    private boolean isAdmin = false;

    @JsonProperty("is_locked")
    @Builder.Default
    private boolean isLocked = false;

    @Builder.Default
    private String permissions = "";

    @Builder.Default
    private Map<String, Object> notifications = Collections.emptyMap();

    @Builder.Default
    private Map<String, Object> settings = Collections.emptyMap();

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;
}
