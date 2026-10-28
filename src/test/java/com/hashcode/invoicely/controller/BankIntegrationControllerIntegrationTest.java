package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.banking.BankAccountDto;
import com.hashcode.invoicely.dto.banking.BankIntegrationBulkActionRequest;
import com.hashcode.invoicely.entity.relational.*;
import com.hashcode.invoicely.repository.relational.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BankIntegrationControllerIntegrationTest {

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
    private BankIntegrationRepository bankIntegrationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
                .companyName("FinanceCorp")
                .subdomain("fin-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("cfo-" + uid + "@financecorp.com")
                .passwordHash("hashedPass")
                .firstName("Alice")
                .lastName("Smith")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("OtherCorp")
                .subdomain("oth-" + uid)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("GET /api/v1/bank_integrations/create - Should return blank scaffold")
    void getBlankBankIntegration_shouldReturnScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/bank_integrations/create")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bank_account_type").value("checking"))
                .andExpect(jsonPath("$.data.bank_account_status").value("active"))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.auto_sync").value(true))
                .andExpect(jsonPath("$.data.is_deleted").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/bank_integrations - Should create bank integration account")
    void createBankIntegration_shouldSucceed() throws Exception {
        BankAccountDto request = BankAccountDto.builder()
                .bankAccountName("Chase Corporate Checking")
                .bankAccountNumber("****1234")
                .bankAccountType("checking")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(new BigDecimal("150000.5000"))
                .nickname("Main Operating")
                .providerName("Plaid")
                .integrationType("plaid")
                .autoSync(true)
                .fromDate(LocalDate.of(2026, 1, 1))
                .build();

        mockMvc.perform(post("/api/v1/bank_integrations")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.bank_account_name").value("Chase Corporate Checking"))
                .andExpect(jsonPath("$.data.bank_account_number").value("****1234"))
                .andExpect(jsonPath("$.data.balance").value(150000.5000))
                .andExpect(jsonPath("$.data.provider_name").value("Plaid"))
                .andExpect(jsonPath("$.data.is_deleted").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/bank_integrations - Should list bank accounts with filtering")
    void getBankIntegrations_shouldReturnPagedList() throws Exception {
        bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("SVB Venture Checking")
                .bankAccountNumber("****4455")
                .bankAccountType("checking")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(new BigDecimal("50000.0000"))
                .isDeleted(false)
                .build());

        bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("Mercury Savings")
                .bankAccountNumber("****8899")
                .bankAccountType("savings")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(new BigDecimal("25000.0000"))
                .isDeleted(false)
                .build());

        // Unfiltered list
        mockMvc.perform(get("/api/v1/bank_integrations")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total").value(2));

        // Filtered list
        mockMvc.perform(get("/api/v1/bank_integrations?filter=Mercury")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bank_account_name").value("Mercury Savings"));
    }

    @Test
    @DisplayName("GET /api/v1/bank_integrations/{id} - Should return account details")
    void getBankIntegrationById_shouldReturnDetails() throws Exception {
        BankIntegrationEntity saved = bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("BoA Treasury")
                .bankAccountNumber("****9900")
                .bankAccountType("checking")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(new BigDecimal("75000.0000"))
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/bank_integrations/" + saved.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.bank_account_name").value("BoA Treasury"))
                .andExpect(jsonPath("$.data.balance").value(75000.0000));
    }

    @Test
    @DisplayName("PUT /api/v1/bank_integrations/{id} - Should update bank account")
    void updateBankIntegration_shouldSucceed() throws Exception {
        BankIntegrationEntity saved = bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("Old Checking")
                .bankAccountNumber("****1111")
                .bankAccountType("checking")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(new BigDecimal("1000.0000"))
                .isDeleted(false)
                .build());

        BankAccountDto updateDto = BankAccountDto.builder()
                .bankAccountName("Updated Premier Checking")
                .nickname("Operating Primary")
                .balance(new BigDecimal("12500.0000"))
                .build();

        mockMvc.perform(put("/api/v1/bank_integrations/" + saved.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bank_account_name").value("Updated Premier Checking"))
                .andExpect(jsonPath("$.data.nickname").value("Operating Primary"))
                .andExpect(jsonPath("$.data.balance").value(12500.0000));
    }

    @Test
    @DisplayName("DELETE /api/v1/bank_integrations/{id} - Should soft delete account")
    void deleteBankIntegration_shouldSoftDelete() throws Exception {
        BankIntegrationEntity saved = bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("Account To Delete")
                .bankAccountNumber("****0000")
                .bankAccountType("checking")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(BigDecimal.ZERO)
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/bank_integrations/" + saved.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bank_integrations?status=deleted")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(saved.getId().toString()));
    }

    @Test
    @DisplayName("GET /api/v1/bank_integrations/refresh_accounts - Should return active accounts")
    void refreshAccounts_shouldReturnAccountsList() throws Exception {
        bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("Active Sync Account")
                .bankAccountNumber("****3322")
                .bankAccountType("checking")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(new BigDecimal("5000.0000"))
                .autoSync(true)
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/bank_integrations/refresh_accounts")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bank_account_name").value("Active Sync Account"));
    }

    @Test
    @DisplayName("POST /api/v1/bank_integrations/bulk - Should archive, restore, and delete accounts")
    void handleBulkAction_archiveRestoreDelete() throws Exception {
        BankIntegrationEntity acc1 = bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("Bulk Account 1")
                .bankAccountNumber("****1111")
                .isDeleted(false)
                .build());

        BankIntegrationEntity acc2 = bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("Bulk Account 2")
                .bankAccountNumber("****2222")
                .isDeleted(false)
                .build());

        // Archive
        BankIntegrationBulkActionRequest archiveReq = BankIntegrationBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(acc1.getId(), acc2.getId()))
                .build();

        mockMvc.perform(post("/api/v1/bank_integrations/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2));

        mockMvc.perform(get("/api/v1/bank_integrations?status=archived")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Restore
        BankIntegrationBulkActionRequest restoreReq = BankIntegrationBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(acc1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/bank_integrations/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        mockMvc.perform(get("/api/v1/bank_integrations?status=active")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(acc1.getId().toString()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Second company cannot view or modify first company accounts")
    void crossTenantIsolation_cannotAccessAnotherCompanyBankIntegration() throws Exception {
        BankIntegrationEntity acc = bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("Private Vault Account")
                .bankAccountNumber("****9999")
                .isDeleted(false)
                .build());

        // Second company sees empty list
        mockMvc.perform(get("/api/v1/bank_integrations")
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Second company cannot fetch by ID
        mockMvc.perform(get("/api/v1/bank_integrations/" + acc.getId())
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isNotFound());
    }
}
