package com.hashcode.invoicely.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginPrecheckResponse {

    @Builder.Default
    private List<String> methods = List.of("password");

    @JsonProperty("secret_required")
    @Builder.Default
    private boolean secretRequired = false;

    @JsonProperty("auth_provider")
    private String authProvider;
}
