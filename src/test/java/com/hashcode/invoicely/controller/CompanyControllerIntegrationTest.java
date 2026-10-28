package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.company.CreateCompanyRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanyControllerIntegrationTest {

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
                .companyName("First Company")
                .subdomain("first-comp")
                .currencyId("USD")
                .countryId("US")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("company.admin@invoicely.com")
                .firstName("Company")
                .lastName("Admin")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("GET /api/v1/companies should return list of companies with pagination metadata")
    void shouldReturnCompanyList() throws Exception {
        mockMvc.perform(get("/api/v1/companies")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].company_name", is("First Company")))
                .andExpect(jsonPath("$.meta.pagination.total", is(1)))
                .andExpect(jsonPath("$.meta.pagination.per_page", is(20)));
    }

    @Test
    @DisplayName("POST /api/v1/companies should create second tenant company and link owner")
    void shouldCreateCompany() throws Exception {
        CreateCompanyRequest request = CreateCompanyRequest.builder()
                .companyName("Second Venture LLC")
                .subdomain("second-venture")
                .currencyId("EUR")
                .countryId("DE")
                .build();

        mockMvc.perform(post("/api/v1/companies")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.company_name", is("Second Venture LLC")))
                .andExpect(jsonPath("$.data.currency_id", is("EUR")));
    }

    @Test
    @DisplayName("GET /api/v1/companies/{id} should return single company detail")
    void shouldGetCompanyById() throws Exception {
        mockMvc.perform(get("/api/v1/companies/" + testCompany.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(testCompany.getId().toString())))
                .andExpect(jsonPath("$.data.company_name", is("First Company")));
    }

    @Test
    @DisplayName("PUT /api/v1/companies/{id} should update company attributes")
    void shouldUpdateCompany() throws Exception {
        CreateCompanyRequest request = CreateCompanyRequest.builder()
                .companyName("Renamed Company Corp")
                .currencyId("GBP")
                .countryId("GB")
                .vatNumber("GB123456789")
                .build();

        mockMvc.perform(put("/api/v1/companies/" + testCompany.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.company_name", is("Renamed Company Corp")))
                .andExpect(jsonPath("$.data.vat_number", is("GB123456789")));
    }
}
