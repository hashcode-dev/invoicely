package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
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

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanyUserControllerIntegrationTest {

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

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(AccountEntity.builder()
                .plan("enterprise")
                .build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Test Preferences Org")
                .subdomain("test-pref-" + UUID.randomUUID().toString().substring(0, 6))
                .currencyId("USD")
                .countryId("US")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("preftest-" + UUID.randomUUID().toString().substring(0, 6) + "@invoicely.com")
                .firstName("Pref")
                .lastName("User")
                .authProvider(AuthProvider.PASSWORD)
                .organizationId(testAccount.getId())
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .permissions(-1L)
                .preferences("{\"dark_mode\":true,\"accent_color\":\"#004bca\"}")
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("GET /api/v1/company_users/{userId}/preferences retrieves persisted user preferences")
    void getPreferences_Success() throws Exception {
        mockMvc.perform(get("/api/v1/company_users/" + testUser.getId() + "/preferences")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dark_mode", is(true)))
                .andExpect(jsonPath("$.data.accent_color", is("#004bca")));
    }

    @Test
    @DisplayName("PUT /api/v1/company_users/{userId}/preferences updates react_settings")
    void updatePreferences_Success() throws Exception {
        Map<String, Object> updatePayload = Map.of(
                "react_settings", Map.of(
                        "dark_mode", false,
                        "show_pdf_preview", true,
                        "show_mini_sidebar", true
                )
        );

        mockMvc.perform(put("/api/v1/company_users/" + testUser.getId() + "/preferences")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.react_settings.dark_mode", is(false)))
                .andExpect(jsonPath("$.data.react_settings.show_mini_sidebar", is(true)));
    }
}
