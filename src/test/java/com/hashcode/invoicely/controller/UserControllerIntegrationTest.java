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
}
