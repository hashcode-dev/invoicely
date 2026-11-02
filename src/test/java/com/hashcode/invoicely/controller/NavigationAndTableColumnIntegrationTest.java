package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.navigation.NavigationConfigDto;
import com.hashcode.invoicely.dto.table.TableColumnConfigDto;
import com.hashcode.invoicely.entity.relational.AccountEntity;
import com.hashcode.invoicely.entity.relational.AuthProvider;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.UserEntity;
import com.hashcode.invoicely.repository.relational.AccountRepository;
import com.hashcode.invoicely.repository.relational.CompanyRepository;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import com.hashcode.invoicely.repository.relational.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NavigationAndTableColumnIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyUserRepository companyUserRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity testAdminUser;
    private UserEntity testEmployeeUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private String adminJwtToken;
    private String employeeJwtToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Nav & Column Org")
                .subdomain("nav-col-" + UUID.randomUUID().toString().substring(0, 6))
                .currencyId("USD")
                .countryId("US")
                .build());

        testAdminUser = userRepository.save(UserEntity.builder()
                .email("admin-" + UUID.randomUUID().toString().substring(0, 6) + "@invoicely.com")
                .firstName("Admin")
                .lastName("Boss")
                .authProvider(AuthProvider.PASSWORD)
                .organizationId(testAccount.getId())
                .build());

        testEmployeeUser = userRepository.save(UserEntity.builder()
                .email("emp-" + UUID.randomUUID().toString().substring(0, 6) + "@invoicely.com")
                .firstName("Emp")
                .lastName("Staff")
                .authProvider(AuthProvider.PASSWORD)
                .organizationId(testAccount.getId())
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testAdminUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .permissions(-1L)
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testEmployeeUser.getId())
                .accountId(testAccount.getId())
                .isOwner(false)
                .isAdmin(false)
                .permissions(1L)
                .build());

        adminJwtToken = jwtTokenProvider.generateToken(testAdminUser.getId(), testAdminUser.getEmail(), testCompany.getId(), testAccount.getId());
        employeeJwtToken = jwtTokenProvider.generateToken(testEmployeeUser.getId(), testEmployeeUser.getEmail(), testCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("GET /api/v1/navigation/config returns distinct visibility for Admin vs Employee")
    void getNavigationConfig_RoleDifferentiation() throws Exception {
        // Admin sees employees and reports
        mockMvc.perform(get("/api/v1/navigation/config")
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employees.is_visible", is(true)))
                .andExpect(jsonPath("$.data.reports.is_visible", is(true)));

        // Employee has reports and employees hidden by default
        mockMvc.perform(get("/api/v1/navigation/config")
                        .header("Authorization", "Bearer " + employeeJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employees.is_visible", is(false)))
                .andExpect(jsonPath("$.data.reports.is_visible", is(false)))
                .andExpect(jsonPath("$.data.invoices.is_visible", is(true)));
    }

    @Test
    @DisplayName("PUT /api/v1/navigation/rules allows admin to customize navigation items")
    void saveNavigationRules_Success() throws Exception {
        List<NavigationConfigDto> customRules = List.of(
                NavigationConfigDto.builder()
                        .itemKey("invoices")
                        .isVisible(true)
                        .displayOrder(1)
                        .customLabel("Billings & Invoices")
                        .isLocked(true)
                        .build(),
                NavigationConfigDto.builder()
                        .itemKey("dashboard")
                        .isVisible(false)
                        .displayOrder(2)
                        .build()
        );

        mockMvc.perform(put("/api/v1/navigation/rules?role=ADMIN")
                        .header("X-Api-Token", adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customRules)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].custom_label", is("Billings & Invoices")))
                .andExpect(jsonPath("$.data[0].is_locked", is(true)));
    }

    @Test
    @DisplayName("GET & PUT /api/v1/companies/{id}/table-columns/{table} manages table column governance")
    void tableColumnGovernance_Success() throws Exception {
        // 1. GET default columns for invoice table
        mockMvc.perform(get("/api/v1/companies/" + testCompany.getId() + "/table-columns/invoice")
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.table_name", is("invoice")))
                .andExpect(jsonPath("$.data.locked_columns", hasItem("number")));

        // 2. PUT custom column governance
        TableColumnConfigDto updateDto = TableColumnConfigDto.builder()
                .tableName("invoice")
                .defaultColumns(List.of("number", "client", "amount", "status"))
                .lockedColumns(List.of("number", "status"))
                .restrictedForEmployees(List.of("internal_cost", "profit_margin"))
                .allowEmployeeCustomization(false)
                .build();

        mockMvc.perform(put("/api/v1/companies/" + testCompany.getId() + "/table-columns/invoice")
                        .header("X-Api-Token", adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.default_columns", hasSize(4)))
                .andExpect(jsonPath("$.data.locked_columns", hasItem("status")))
                .andExpect(jsonPath("$.data.allow_employee_customization", is(false)));
    }
}
