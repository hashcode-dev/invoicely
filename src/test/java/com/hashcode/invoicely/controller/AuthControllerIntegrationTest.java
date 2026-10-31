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

    @Test
    @DisplayName("POST /api/v1/register should register a new conventional user and return tokens and company")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        com.hashcode.invoicely.dto.auth.RegisterRequest request = com.hashcode.invoicely.dto.auth.RegisterRequest.builder()
                .email("new.conventional@invoicely.com")
                .password("SecurePass789!")
                .firstName("Alice")
                .lastName("Smith")
                .companyName("Alice Consulting")
                .build();

        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].user.email", is("new.conventional@invoicely.com")))
                .andExpect(jsonPath("$.data[0].user.auth_provider", is("PASSWORD")))
                .andExpect(jsonPath("$.data[0].company.company_name", is("Alice Consulting")))
                .andExpect(jsonPath("$.data[0].token.token").isString())
                .andExpect(jsonPath("$.data[0].is_owner", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/register should fail with 409 Conflict when email already registered via password")
    void shouldFailConventionalRegistrationWhenEmailExistsWithPassword() throws Exception {
        com.hashcode.invoicely.dto.auth.RegisterRequest request = com.hashcode.invoicely.dto.auth.RegisterRequest.builder()
                .email("test.manager@invoicely.com")
                .password("AnotherPassword123!")
                .firstName("Test")
                .lastName("Manager")
                .build();

        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code", is("AUTH_PROVIDER_MISMATCH")))
                .andExpect(jsonPath("$.required_provider", is("PASSWORD")))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("POST /api/v1/register should fail with 409 Conflict when user registered via Google Sign-In")
    void shouldFailConventionalRegistrationWhenEmailExistsWithGoogle() throws Exception {
        userRepository.save(UserEntity.builder()
                .email("google.user@invoicely.com")
                .firstName("Google")
                .lastName("User")
                .authProvider(com.hashcode.invoicely.entity.relational.AuthProvider.GOOGLE)
                .oauthId("google-12345")
                .isRegistered(true)
                .build());

        com.hashcode.invoicely.dto.auth.RegisterRequest request = com.hashcode.invoicely.dto.auth.RegisterRequest.builder()
                .email("google.user@invoicely.com")
                .password("SomePassword123!")
                .firstName("Google")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code", is("AUTH_PROVIDER_MISMATCH")))
                .andExpect(jsonPath("$.required_provider", is("GOOGLE")))
                .andExpect(jsonPath("$.message", is("An account with this email is already registered using Google Sign-In. Please use the 'Sign in with Google' option.")));
    }

    @Test
    @DisplayName("POST /api/v1/login should fail with 409 Conflict when user registered via Google Sign-In")
    void shouldFailConventionalLoginWhenUserRegisteredWithGoogle() throws Exception {
        userRepository.save(UserEntity.builder()
                .email("google.only@invoicely.com")
                .firstName("Google")
                .lastName("User")
                .authProvider(com.hashcode.invoicely.entity.relational.AuthProvider.GOOGLE)
                .oauthId("google-sub-999")
                .isRegistered(true)
                .build());

        LoginRequest request = LoginRequest.builder()
                .email("google.only@invoicely.com")
                .password("AnyPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code", is("AUTH_PROVIDER_MISMATCH")))
                .andExpect(jsonPath("$.required_provider", is("GOOGLE")))
                .andExpect(jsonPath("$.message", is("An account with this email is already registered using Google Sign-In. Please use the 'Sign in with Google' option.")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/google should register and provision a new user via Google Sign-In")
    void shouldLoginOrRegisterViaGoogleSuccessfully() throws Exception {
        com.hashcode.invoicely.dto.auth.GoogleLoginRequest request = com.hashcode.invoicely.dto.auth.GoogleLoginRequest.builder()
                .email("brandnew.google@invoicely.com")
                .firstName("David")
                .lastName("Miller")
                .avatarUrl("https://example.com/avatar.jpg")
                .sub("google-oauth-1001")
                .build();

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].user.email", is("brandnew.google@invoicely.com")))
                .andExpect(jsonPath("$.data[0].user.auth_provider", is("GOOGLE")))
                .andExpect(jsonPath("$.data[0].user.avatar", is("https://example.com/avatar.jpg")))
                .andExpect(jsonPath("$.data[0].token.token").isString())
                .andExpect(jsonPath("$.data[0].is_owner", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/google should reject with 409 Conflict when user registered via password")
    void shouldFailGoogleLoginWhenUserRegisteredWithPassword() throws Exception {
        com.hashcode.invoicely.dto.auth.GoogleLoginRequest request = com.hashcode.invoicely.dto.auth.GoogleLoginRequest.builder()
                .email("test.manager@invoicely.com")
                .firstName("Test")
                .lastName("Manager")
                .sub("google-oauth-duplicate")
                .build();

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code", is("AUTH_PROVIDER_MISMATCH")))
                .andExpect(jsonPath("$.required_provider", is("PASSWORD")))
                .andExpect(jsonPath("$.message", is("This account is registered using email and password. Please sign in using your email and password.")));
    }

    @Test
    @DisplayName("POST /api/v1/login/precheck should return provider details for Google and Password users")
    void shouldReturnCorrectPrecheckAuthProvider() throws Exception {
        userRepository.save(UserEntity.builder()
                .email("existing.google@invoicely.com")
                .firstName("Google")
                .lastName("Account")
                .authProvider(com.hashcode.invoicely.entity.relational.AuthProvider.GOOGLE)
                .isRegistered(true)
                .build());

        // Check Google user
        LoginPrecheckRequest googlePrecheck = LoginPrecheckRequest.builder()
                .email("existing.google@invoicely.com")
                .build();

        mockMvc.perform(post("/api/v1/login/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(googlePrecheck)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods", hasItem("google")))
                .andExpect(jsonPath("$.auth_provider", is("GOOGLE")));

        // Check Password user
        LoginPrecheckRequest passwordPrecheck = LoginPrecheckRequest.builder()
                .email("test.manager@invoicely.com")
                .build();

        mockMvc.perform(post("/api/v1/login/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordPrecheck)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods", hasItem("password")))
                .andExpect(jsonPath("$.auth_provider", is("PASSWORD")));

        // Check non-existent user
        LoginPrecheckRequest newPrecheck = LoginPrecheckRequest.builder()
                .email("unknown@invoicely.com")
                .build();

        mockMvc.perform(post("/api/v1/login/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPrecheck)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods", hasItem("password")))
                .andExpect(jsonPath("$.methods", hasItem("google")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/employee_login should authenticate employee with Organization ID, Employee ID, and Password")
    void shouldAuthenticateEmployeeSuccessfully() throws Exception {
        com.hashcode.invoicely.dto.auth.EmployeeLoginRequest request = com.hashcode.invoicely.dto.auth.EmployeeLoginRequest.builder()
                .organizationId(testAccount.getId().toString())
                .employeeId("test.manager@invoicely.com")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/employee_login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].user.email", is("test.manager@invoicely.com")))
                .andExpect(jsonPath("$.data[0].account.id", is(testAccount.getId().toString())))
                .andExpect(jsonPath("$.data[0].token.token").isString());
    }

    @Test
    @DisplayName("POST /api/v1/auth/employee_login should reject when organization ID is invalid")
    void shouldRejectEmployeeLoginWithInvalidOrganization() throws Exception {
        com.hashcode.invoicely.dto.auth.EmployeeLoginRequest request = com.hashcode.invoicely.dto.auth.EmployeeLoginRequest.builder()
                .organizationId(java.util.UUID.randomUUID().toString())
                .employeeId("test.manager@invoicely.com")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/employee_login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Organization not found")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/employee_login should reject when employee does not belong to organization")
    void shouldRejectEmployeeLoginWhenNotMemberOfOrg() throws Exception {
        AccountEntity otherAccount = accountRepository.save(AccountEntity.builder().plan("free").build());

        com.hashcode.invoicely.dto.auth.EmployeeLoginRequest request = com.hashcode.invoicely.dto.auth.EmployeeLoginRequest.builder()
                .organizationId(otherAccount.getId().toString())
                .employeeId("test.manager@invoicely.com")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/employee_login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("not assigned to any active company")));
    }
}
