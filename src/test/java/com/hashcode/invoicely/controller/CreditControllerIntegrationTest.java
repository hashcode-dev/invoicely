package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.invoice.CreditDto;
import com.hashcode.invoicely.entity.relational.*;
import com.hashcode.invoicely.repository.relational.*;
import com.hashcode.invoicely.service.SequenceGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CreditControllerIntegrationTest {

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
    private ClientRepository clientRepository;

    @Autowired
    private CreditRepository creditRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SequenceGeneratorService sequenceGeneratorService;

    private final AtomicInteger seqCounter = new AtomicInteger(900);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private ClientEntity testClient;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Credits")
                .subdomain("acme-credits")
                .currencyId("USD")
                .countryId("US")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("credits@invoicely.com")
                .firstName("Credit")
                .lastName("Manager")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .build());

        testClient = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("C003")
                .name("Credited Client Inc")
                .currencyId("USD")
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());

        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    @Test
    @DisplayName("GET /api/v1/credits/create should return default blank credit")
    void shouldReturnBlankCredit() throws Exception {
        mockMvc.perform(get("/api/v1/credits/create?client_id=" + testClient.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.client_id", is(testClient.getId().toString())));
    }

    @Test
    @DisplayName("POST /api/v1/credits should issue credit note and increment client credit balance")
    void shouldCreateCreditAndAdjustClientBalance() throws Exception {
        CreditDto request = CreditDto.builder()
                .clientId(testClient.getId())
                .date("2026-10-28")
                .amount(new BigDecimal("150.0000"))
                .publicNotes("Satisfaction Guarantee Credit")
                .build();

        mockMvc.perform(post("/api/v1/credits")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.amount", is(150.0)))
                .andExpect(jsonPath("$.data.balance", is(150.0)));

        // Verify client's credit balance updated
        ClientEntity reloadedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("150.0000").compareTo(reloadedClient.getCreditBalance()));
    }

    @Test
    @DisplayName("GET /api/v1/credits and PUT /api/v1/credits/{id}")
    void shouldListAndUpdateCredits() throws Exception {
        CreditEntity credit = creditRepository.save(CreditEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("CR-001")
                .date(java.time.LocalDate.now())
                .amount(new BigDecimal("75.0000"))
                .balance(new BigDecimal("75.0000"))
                .build());

        mockMvc.perform(get("/api/v1/credits")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].number", is("CR-001")));

        CreditDto updateReq = CreditDto.builder()
                .terms("Updated Credit Terms")
                .amount(new BigDecimal("100.0000"))
                .build();

        mockMvc.perform(put("/api/v1/credits/" + credit.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.terms", is("Updated Credit Terms")))
                .andExpect(jsonPath("$.data.amount", is(100.0)));
    }
}
