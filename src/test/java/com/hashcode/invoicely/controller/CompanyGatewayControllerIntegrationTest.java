package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.payment.CompanyGatewayBulkActionRequest;
import com.hashcode.invoicely.dto.payment.CompanyGatewayDto;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CompanyGatewayControllerIntegrationTest {

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
    private CompanyGatewayRepository companyGatewayRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SequenceGeneratorService sequenceGeneratorService;

    private final AtomicInteger seqCounter = new AtomicInteger(100);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private String jwtToken;

    private CompanyEntity secondCompany;
    private String secondJwtToken;

    @BeforeEach
    void setUp() {
        String uid = java.util.UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Payments Inc")
                .subdomain("acme-gw-" + uid)
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Beta Gateways LLC")
                .subdomain("beta-gw-" + uid)
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("gateway." + uid + "@invoicely.com")
                .firstName("Gateway")
                .lastName("Admin")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());

        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    @Test
    @DisplayName("GET /api/v1/company_gateways/create should return blank gateway scaffold")
    void shouldReturnBlankGateway() throws Exception {
        mockMvc.perform(get("/api/v1/company_gateways/create")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gateway_key", is("")))
                .andExpect(jsonPath("$.data.label", is("")))
                .andExpect(jsonPath("$.data.is_deleted", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/company_gateways should create gateway successfully")
    void shouldCreateCompanyGateway() throws Exception {
        CompanyGatewayDto dto = CompanyGatewayDto.builder()
                .gatewayKey("stripe")
                .label("Primary Stripe Gateway")
                .config("{\"publishable_key\":\"pk_test_123\",\"secret_key\":\"sk_test_456\"}")
                .build();

        mockMvc.perform(post("/api/v1/company_gateways")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.gateway_key", is("stripe")))
                .andExpect(jsonPath("$.data.label", is("Primary Stripe Gateway")))
                .andExpect(jsonPath("$.data.is_deleted", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/company_gateways should list active gateways")
    void shouldListCompanyGateways() throws Exception {
        CompanyGatewayEntity gw = companyGatewayRepository.save(CompanyGatewayEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .gatewayKey("paypal")
                .label("PayPal Express")
                .config("{\"client_id\":\"paypal_123\"}")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/company_gateways")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].gateway_key", is("paypal")));
    }

    @Test
    @DisplayName("PUT /api/v1/company_gateways/{id} should update existing gateway")
    void shouldUpdateCompanyGateway() throws Exception {
        CompanyGatewayEntity gw = companyGatewayRepository.save(CompanyGatewayEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .gatewayKey("stripe")
                .label("Initial Label")
                .config("{\"key\":\"val\"}")
                .isDeleted(false)
                .build());

        CompanyGatewayDto updateDto = CompanyGatewayDto.builder()
                .label("Updated Stripe Gateway")
                .config("{\"key\":\"new_val\"}")
                .build();

        mockMvc.perform(put("/api/v1/company_gateways/" + gw.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label", is("Updated Stripe Gateway")))
                .andExpect(jsonPath("$.data.config", containsString("new_val")));
    }

    @Test
    @DisplayName("DELETE /api/v1/company_gateways/{id} should soft delete gateway")
    void shouldSoftDeleteCompanyGateway() throws Exception {
        CompanyGatewayEntity gw = companyGatewayRepository.save(CompanyGatewayEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .gatewayKey("authorize_net")
                .label("Authorize.Net")
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/company_gateways/" + gw.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        CompanyGatewayEntity deleted = companyGatewayRepository.findById(gw.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(deleted.isDeleted());
    }

    @Test
    @DisplayName("POST /api/v1/company_gateways/bulk should archive and restore gateways")
    void shouldHandleBulkActions() throws Exception {
        CompanyGatewayEntity gw = companyGatewayRepository.save(CompanyGatewayEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .gatewayKey("braintree")
                .label("Braintree Gateway")
                .isDeleted(false)
                .build());

        CompanyGatewayBulkActionRequest archiveReq = CompanyGatewayBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(gw.getId()))
                .build();

        mockMvc.perform(post("/api/v1/company_gateways/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].is_deleted", is(true)));

        CompanyGatewayBulkActionRequest restoreReq = CompanyGatewayBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(gw.getId()))
                .build();

        mockMvc.perform(post("/api/v1/company_gateways/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].is_deleted", is(false)));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B should not access Tenant A gateway")
    void shouldEnforceTenantIsolation() throws Exception {
        CompanyGatewayEntity gwA = companyGatewayRepository.save(CompanyGatewayEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .gatewayKey("tenant_a_gateway")
                .label("Tenant A Gateway")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/company_gateways/" + gwA.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
