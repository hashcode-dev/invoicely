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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPortalControllerIntegrationTest {

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

    private UserEntity superAdminUser;
    private CompanyEntity customerCompany;
    private AccountEntity customerAccount;
    private String adminJwtToken;

    @BeforeEach
    void setUp() {
        customerAccount = accountRepository.save(AccountEntity.builder().plan("pro").build());

        customerCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(customerAccount.getId())
                .companyName("Acme Corp")
                .subdomain("acme-" + UUID.randomUUID().toString().substring(0, 6))
                .currencyId("USD")
                .countryId("US")
                .build());

        superAdminUser = userRepository.save(UserEntity.builder()
                .email("operator-" + UUID.randomUUID().toString().substring(0, 6) + "@invoicely.com")
                .firstName("Operator")
                .lastName("Root")
                .authProvider(AuthProvider.PASSWORD)
                .organizationId(customerAccount.getId())
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(customerCompany.getId())
                .userId(superAdminUser.getId())
                .accountId(customerAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .permissions(-1L)
                .build());

        adminJwtToken = jwtTokenProvider.generateToken(superAdminUser.getId(), superAdminUser.getEmail(), customerCompany.getId(), customerAccount.getId());
    }

    @Test
    @DisplayName("GET /api/v1/admin/metrics returns real-time SaaS platform telemetry")
    void getMetrics_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics")
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_tenants", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.total_active_users", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/admin/customers lists tenant organizations and revenue details")
    void getCustomers_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("X-Api-Token", adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].company_name", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/admin/customers/{id}/impersonate issues tenant session with audit log")
    void impersonateCustomer_Success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/customers/" + customerCompany.getId() + "/impersonate")
                        .header("X-Api-Token", adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    @DisplayName("Support Ticket Lifecycle: Create, List, Message Thread and Reply")
    void supportTicketLifecycle_Success() throws Exception {
        // 1. Create ticket
        Map<String, String> ticketRequest = Map.of(
                "account_id", customerAccount.getId().toString(),
                "company_id", customerCompany.getId().toString(),
                "subject", "Issue with Stripe gateway webhook",
                "priority", "HIGH",
                "category", "BILLING",
                "message", "Our webhook is returning 400 Bad Request on payment capture"
        );

        String createResponse = mockMvc.perform(post("/api/v1/admin/support/tickets")
                        .header("X-Api-Token", adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticketRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket_number", startsWith("TICK-")))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andReturn().getResponse().getContentAsString();

        String ticketId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        // 2. Query ticket list
        mockMvc.perform(get("/api/v1/admin/support/tickets?status=OPEN")
                        .header("X-Api-Token", adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // 3. Operator replies to ticket and resolves it
        Map<String, Object> replyRequest = Map.of(
                "message", "We verified the webhook signature secret. Please refresh your API token in Settings.",
                "is_internal_note", false,
                "status", "RESOLVED"
        );

        mockMvc.perform(post("/api/v1/admin/support/tickets/" + ticketId + "/reply")
                        .header("X-Api-Token", adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message_body", containsString("webhook signature secret")));

        // 4. Verify message thread has 2 messages (initial + operator reply)
        mockMvc.perform(get("/api/v1/admin/support/tickets/" + ticketId + "/messages")
                        .header("X-Api-Token", adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }
}
