package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryDto;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ExpenseCategoryControllerIntegrationTest {

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
    private ExpenseCategoryRepository expenseCategoryRepository;

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
        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("ProcureCo")
                .subdomain("procure-" + uid)
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("OtherProcure")
                .subdomain("other-procure-" + uid)
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("buyer." + uid + "@invoicely.com")
                .firstName("Procure")
                .lastName("Buyer")
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
    @DisplayName("POST /api/v1/expense_categories should create category successfully")
    void shouldCreateExpenseCategory() throws Exception {
        ExpenseCategoryDto dto = ExpenseCategoryDto.builder()
                .name("Office Supplies")
                .color("#FF5733")
                .build();

        mockMvc.perform(post("/api/v1/expense_categories")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is("Office Supplies")))
                .andExpect(jsonPath("$.data.color", is("#FF5733")));
    }

    @Test
    @DisplayName("GET /api/v1/expense_categories should list active categories")
    void shouldListExpenseCategories() throws Exception {
        expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Travel & Lodging")
                .color("#3380FF")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/expense_categories")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].name", is("Travel & Lodging")));
    }

    @Test
    @DisplayName("PUT /api/v1/expense_categories/{id} should update category")
    void shouldUpdateExpenseCategory() throws Exception {
        ExpenseCategoryEntity entity = expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Meals")
                .color("#000000")
                .isDeleted(false)
                .build());

        ExpenseCategoryDto updateDto = ExpenseCategoryDto.builder()
                .name("Meals & Entertainment")
                .color("#00FF00")
                .build();

        mockMvc.perform(put("/api/v1/expense_categories/" + entity.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Meals & Entertainment")))
                .andExpect(jsonPath("$.data.color", is("#00FF00")));
    }

    @Test
    @DisplayName("DELETE /api/v1/expense_categories/{id} should soft delete category")
    void shouldSoftDeleteExpenseCategory() throws Exception {
        ExpenseCategoryEntity entity = expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Subscriptions")
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/expense_categories/" + entity.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ExpenseCategoryEntity deleted = expenseCategoryRepository.findById(entity.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(deleted.isDeleted());
    }

    @Test
    @DisplayName("POST /api/v1/expense_categories/bulk should archive and restore categories")
    void shouldBulkArchiveAndRestoreCategories() throws Exception {
        ExpenseCategoryEntity entity = expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Hardware")
                .isDeleted(false)
                .build());

        ExpenseCategoryBulkActionRequest archiveReq = ExpenseCategoryBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(entity.getId()))
                .build();

        mockMvc.perform(post("/api/v1/expense_categories/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].archived_at", notNullValue()));

        ExpenseCategoryBulkActionRequest restoreReq = ExpenseCategoryBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(entity.getId()))
                .build();

        mockMvc.perform(post("/api/v1/expense_categories/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].archived_at", nullValue()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot access Tenant A category")
    void shouldEnforceTenantIsolation() throws Exception {
        ExpenseCategoryEntity entity = expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Internal Project R&D")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/expense_categories/" + entity.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
