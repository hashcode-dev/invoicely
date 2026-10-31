package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.auth.CompanyUserResponse;
import com.hashcode.invoicely.dto.auth.LoginPrecheckRequest;
import com.hashcode.invoicely.dto.auth.LoginPrecheckResponse;
import com.hashcode.invoicely.dto.auth.LoginRequest;
import com.hashcode.invoicely.dto.common.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    LoginPrecheckResponse precheck(LoginPrecheckRequest request);

    ApiResponse<List<CompanyUserResponse>> login(LoginRequest request);

    ApiResponse<List<CompanyUserResponse>> refresh(UUID currentCompanyId);
}
