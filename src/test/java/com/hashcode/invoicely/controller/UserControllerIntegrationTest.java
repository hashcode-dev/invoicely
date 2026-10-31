package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.user.CreateUserRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

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
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());
        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Team Company")
                .subdomain("team-comp")
                .currencyId("USD")
                .countryId("US")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("owner@invoicely.com")
                .firstName("Team")
                .lastName("Owner")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("GET /api/v1/users should return team members in active tenant")
    void shouldReturnUserList() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email", is("owner@invoicely.com")));
    }

    @Test
    @DisplayName("POST /api/v1/users should invite/create new user into active company")
    void shouldCreateUser() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("developer@invoicely.com")
                .firstName("Jane")
                .lastName("Developer")
                .isAdmin(true)
                .permissions(63L)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", is("developer@invoicely.com")))
                .andExpect(jsonPath("$.data.first_name", is("Jane")));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return single user details")
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + testUser.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(testUser.getId().toString())))
                .andExpect(jsonPath("$.data.email", is("owner@invoicely.com")));
    }

    @Test
    @DisplayName("GET /api/v1/users/create should return blank user scaffold with company_user")
    void shouldReturnBlankUserScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/users/create")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organization_id", is(testAccount.getId().toString())))
                .andExpect(jsonPath("$.data.company_user").exists())
                .andExpect(jsonPath("$.data.company_user.is_admin", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} should update user profile and company permissions")
    void shouldUpdateUserAndPermissions() throws Exception {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .email("worker@invoicely.com")
                .firstName("OldFirst")
                .lastName("OldLast")
                .isAdmin(false)
                .permissions(10L)
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/users")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String workerId = objectMapper.readTree(responseStr).get("data").get("id").asText();

        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .email("worker@invoicely.com")
                .firstName("NewFirst")
                .lastName("NewLast")
                .isAdmin(true)
                .permissions(100L)
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/users/" + workerId)
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.first_name", is("NewFirst")))
                .andExpect(jsonPath("$.data.company_user.is_admin", is(true)))
                .andExpect(jsonPath("$.data.company_user.permissions", is("100")));
    }

    @Test
    @DisplayName("POST /api/v1/users/bulk should archive and restore users")
    void shouldBulkArchiveAndRestoreUsers() throws Exception {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .email("bulk.target@invoicely.com")
                .firstName("Bulk")
                .lastName("Target")
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/users")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        java.util.UUID workerId = java.util.UUID.fromString(objectMapper.readTree(responseStr).get("data").get("id").asText());

        com.hashcode.invoicely.dto.user.UserBulkActionRequest archiveReq = com.hashcode.invoicely.dto.user.UserBulkActionRequest.builder()
                .action("archive")
                .ids(java.util.List.of(workerId))
                .build();

        mockMvc.perform(post("/api/v1/users/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk());

        CompanyUserEntity link = companyUserRepository.findByCompanyIdAndUserId(testCompany.getId(), workerId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(link.isLocked());

        com.hashcode.invoicely.dto.user.UserBulkActionRequest restoreReq = com.hashcode.invoicely.dto.user.UserBulkActionRequest.builder()
                .action("restore")
                .ids(java.util.List.of(workerId))
                .build();

        mockMvc.perform(post("/api/v1/users/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk());

        link = companyUserRepository.findByCompanyIdAndUserId(testCompany.getId(), workerId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(link.isLocked());
    }

    @Test
    @DisplayName("Guardrails 1 & 2: Same employee in multiple companies has NO duplicate UserEntity and independent roles")
    void shouldEnforceAntiDuplicateAndScopedRoles() throws Exception {
        // Step 1: Create Company 2 under same account
        CompanyEntity company2 = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Second Company")
                .subdomain("second-comp")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(company2.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .build());

        String jwtCompany2 = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), company2.getId(), testAccount.getId());

        // Step 2: Add employee to Company 1 with Admin=true, permissions=50
        CreateUserRequest req1 = CreateUserRequest.builder()
                .email("shared.employee@invoicely.com")
                .firstName("Shared")
                .lastName("Employee")
                .isAdmin(true)
                .permissions(50L)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.company_user.is_admin", is(true)))
                .andExpect(jsonPath("$.data.company_user.permissions", is("50")));

        // Step 3: Add the SAME employee to Company 2 with Admin=false, permissions=5
        CreateUserRequest req2 = CreateUserRequest.builder()
                .email("shared.employee@invoicely.com")
                .firstName("Shared")
                .lastName("Employee")
                .isAdmin(false)
                .permissions(5L)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("X-Api-Token", jwtCompany2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.company_user.is_admin", is(false)))
                .andExpect(jsonPath("$.data.company_user.permissions", is("5")));

        // Verification 1: Exactly ONE UserEntity exists in database for this email
        java.util.List<UserEntity> allMatches = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase("shared.employee@invoicely.com"))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, allMatches.size(), "Zero duplicate UserEntity records allowed!");

        UserEntity sharedUser = allMatches.get(0);

        // Verification 2: Scoped roles are independent in each company
        CompanyUserEntity linkComp1 = companyUserRepository.findByCompanyIdAndUserId(testCompany.getId(), sharedUser.getId()).orElseThrow();
        CompanyUserEntity linkComp2 = companyUserRepository.findByCompanyIdAndUserId(company2.getId(), sharedUser.getId()).orElseThrow();

        org.junit.jupiter.api.Assertions.assertTrue(linkComp1.isAdmin());
        org.junit.jupiter.api.Assertions.assertEquals(50L, linkComp1.getPermissions());

        org.junit.jupiter.api.Assertions.assertFalse(linkComp2.isAdmin());
        org.junit.jupiter.api.Assertions.assertEquals(5L, linkComp2.getPermissions());
    }

    @Test
    @DisplayName("Guardrail 3a: Non-admin employee cannot create users (403 Forbidden)")
    void shouldBlockNonAdminFromCreatingUser() throws Exception {
        UserEntity regularWorker = userRepository.save(UserEntity.builder()
                .email("regular.worker@invoicely.com")
                .firstName("Regular")
                .lastName("Worker")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(regularWorker.getId())
                .accountId(testAccount.getId())
                .isOwner(false)
                .isAdmin(false)
                .build());

        String workerToken = jwtTokenProvider.generateToken(regularWorker.getId(), regularWorker.getEmail(), testCompany.getId(), testAccount.getId());

        CreateUserRequest req = CreateUserRequest.builder()
                .email("illegal.invite@invoicely.com")
                .firstName("Illegal")
                .lastName("Invite")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("X-Api-Token", workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
