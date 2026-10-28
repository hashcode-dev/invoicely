package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.invoice.TaxRateDto;
import com.hashcode.invoicely.entity.relational.*;
import com.hashcode.invoicely.repository.relational.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaxRateControllerIntegrationTest {

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
    private TaxRateRepository taxRateRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(AccountEntity.builder().plan("pro").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Tax")
                .subdomain("acme-tax")
                .currencyId("USD")
                .countryId("US")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("tax@invoicely.com")
                .firstName("Tax")
                .lastName("Accountant")
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
    @DisplayName("POST /api/v1/tax_rates and GET /api/v1/tax_rates lifecycle")
    void shouldCreateAndListTaxRates() throws Exception {
        TaxRateDto createReq = TaxRateDto.builder()
                .name("State Sales Tax")
                .rate(new BigDecimal("7.2500"))
                .isInclusive(false)
                .build();

        mockMvc.perform(post("/api/v1/tax_rates")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is("State Sales Tax")))
                .andExpect(jsonPath("$.data.rate", is(7.25)));

        mockMvc.perform(get("/api/v1/tax_rates")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("State Sales Tax")));
    }

    @Test
    @DisplayName("PUT /api/v1/tax_rates/{id} and DELETE /api/v1/tax_rates/{id}")
    void shouldUpdateAndDeleteTaxRate() throws Exception {
        TaxRateEntity tax = taxRateRepository.save(TaxRateEntity.builder()
                .companyId(testCompany.getId())
                .name("Old Tax")
                .rate(new BigDecimal("5.0000"))
                .build());

        TaxRateDto updateReq = TaxRateDto.builder()
                .name("Revised Tax")
                .rate(new BigDecimal("6.0000"))
                .isInclusive(true)
                .build();

        mockMvc.perform(put("/api/v1/tax_rates/" + tax.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Revised Tax")))
                .andExpect(jsonPath("$.data.is_inclusive", is(true)));

        mockMvc.perform(delete("/api/v1/tax_rates/" + tax.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify soft-deleted tax is excluded from list
        mockMvc.perform(get("/api/v1/tax_rates")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
