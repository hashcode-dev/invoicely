package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.auth.LoginPrecheckRequest;
import com.hashcode.invoicely.dto.auth.LoginRequest;
import com.hashcode.invoicely.entity.relational.AccountEntity;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

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
                .companyName("Acme Testing Corp")
                .subdomain("acme-test")
                .currencyId("USD")
                .countryId("US")
                .build());

        testAccount.setDefaultCompanyId(testCompany.getId());
        accountRepository.save(testAccount);

        testUser = userRepository.save(UserEntity.builder()
                .email("test.manager@invoicely.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Test")
                .lastName("Manager")
                .isRegistered(true)
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .permissions(-1L)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("POST /api/v1/login/precheck should return available login methods")
    void shouldReturnPrecheckMethods() throws Exception {
        LoginPrecheckRequest request = LoginPrecheckRequest.builder()
                .email("test.manager@invoicely.com")
                .build();

        mockMvc.perform(post("/api/v1/login/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods", hasItem("password")))
                .andExpect(jsonPath("$.secret_required", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/login/precheck should fail with 422 when email is invalid")
    void shouldFailPrecheckOnInvalidEmail() throws Exception {
        LoginPrecheckRequest request = LoginPrecheckRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/v1/login/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/login should authenticate and return CompanyUser array with JWT token")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test.manager@invoicely.com")
                .password("SecretPass123!")
                .tokenName("Web Dashboard")
                .build();

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].user.email", is("test.manager@invoicely.com")))
                .andExpect(jsonPath("$.data[0].company.company_name", is("Acme Testing Corp")))
                .andExpect(jsonPath("$.data[0].token.token").isString())
                .andExpect(jsonPath("$.data[0].is_owner", is(true)))
                .andExpect(jsonPath("$.data[0].is_admin", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/login should return 401 Unauthorized for incorrect password")
    void shouldRejectLoginWithWrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test.manager@invoicely.com")
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/v1/refresh with X-Api-Token should return current authenticated state")
    void shouldRefreshSessionSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/refresh")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].user.email", is("test.manager@invoicely.com")))
                .andExpect(jsonPath("$.data[0].company.id", is(testCompany.getId().toString())));
    }

    @Test
    @DisplayName("POST /api/v1/logout should return success message")
    void shouldLogoutSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/logout")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("logged_out")));
    }
}
