package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleBulkActionRequest;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleDto;
import com.hashcode.invoicely.dto.banking.RuleCriteriaDto;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BankTransactionRuleControllerIntegrationTest {

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
    private BankTransactionRuleRepository ruleRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private String jwtToken;

    private CompanyEntity secondCompany;
    private String secondJwtToken;

    private VendorEntity testVendor;
    private ExpenseCategoryEntity testCategory;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("RuleCorp")
                .subdomain("rul-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("admin-" + uid + "@rulecorp.com")
                .passwordHash("hashedPass")
                .firstName("Rule")
                .lastName("Master")
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

        testVendor = vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .name("AWS Cloud Services")
                .vendorNumber("VND-1001")
                .build());

        testCategory = expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .name("Hosting & Infrastructure")
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/bank_transaction_rules/create - Should return blank scaffold")
    void getBlankRule_shouldReturnScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/bank_transaction_rules/create")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applies_to").value("all"))
                .andExpect(jsonPath("$.data.auto_convert").value(false))
                .andExpect(jsonPath("$.data.matches_on_all").value(true))
                .andExpect(jsonPath("$.data.is_deleted").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transaction_rules - Should create transaction rule with criteria")
    void createRule_shouldSucceed() throws Exception {
        RuleCriteriaDto c1 = RuleCriteriaDto.builder()
                .searchKey("description")
                .operator("contains")
                .value("Amazon Web Services")
                .build();

        RuleCriteriaDto c2 = RuleCriteriaDto.builder()
                .searchKey("amount")
                .operator("greater_than")
                .value("50.00")
                .build();

        BankTransactionRuleDto request = BankTransactionRuleDto.builder()
                .name("Auto Match AWS Invoices")
                .appliesTo("all")
                .rules(List.of(c1, c2))
                .autoConvert(true)
                .matchesOnAll(true)
                .vendorId(testVendor.getId())
                .categoryId(testCategory.getId())
                .build();

        mockMvc.perform(post("/api/v1/bank_transaction_rules")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Auto Match AWS Invoices"))
                .andExpect(jsonPath("$.data.auto_convert").value(true))
                .andExpect(jsonPath("$.data.matches_on_all").value(true))
                .andExpect(jsonPath("$.data.rules", hasSize(2)))
                .andExpect(jsonPath("$.data.vendor_id").value(testVendor.getId().toString()))
                .andExpect(jsonPath("$.data.category_id").value(testCategory.getId().toString()))
                .andExpect(jsonPath("$.data.is_deleted").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/bank_transaction_rules - Should return list of rules with filtering")
    void getRules_shouldReturnPagedList() throws Exception {
        ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Github Subscription Rule")
                .appliesTo("all")
                .autoConvert(true)
                .matchesOnAll(true)
                .isDeleted(false)
                .build());

        ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Slack Team Rule")
                .appliesTo("all")
                .autoConvert(false)
                .matchesOnAll(true)
                .isDeleted(false)
                .build());

        // All rules
        mockMvc.perform(get("/api/v1/bank_transaction_rules")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total").value(2));

        // Filtered
        mockMvc.perform(get("/api/v1/bank_transaction_rules?filter=Github")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Github Subscription Rule"));
    }

    @Test
    @DisplayName("GET /api/v1/bank_transaction_rules/{id} - Should return rule details")
    void getRuleById_shouldReturnDetails() throws Exception {
        BankTransactionRuleEntity saved = ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Stripe Payout Rule")
                .appliesTo("all")
                .autoConvert(false)
                .matchesOnAll(true)
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/bank_transaction_rules/" + saved.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Stripe Payout Rule"));
    }

    @Test
    @DisplayName("PUT /api/v1/bank_transaction_rules/{id} - Should update rule")
    void updateRule_shouldSucceed() throws Exception {
        BankTransactionRuleEntity saved = ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Old Rule Name")
                .appliesTo("all")
                .autoConvert(false)
                .matchesOnAll(false)
                .isDeleted(false)
                .build());

        BankTransactionRuleDto updateDto = BankTransactionRuleDto.builder()
                .name("Updated Rule Name")
                .autoConvert(true)
                .matchesOnAll(true)
                .build();

        mockMvc.perform(put("/api/v1/bank_transaction_rules/" + saved.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Rule Name"))
                .andExpect(jsonPath("$.data.auto_convert").value(true))
                .andExpect(jsonPath("$.data.matches_on_all").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/bank_transaction_rules/{id} - Should soft delete rule")
    void deleteRule_shouldSoftDelete() throws Exception {
        BankTransactionRuleEntity saved = ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Rule to delete")
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/bank_transaction_rules/" + saved.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bank_transaction_rules?status=deleted")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(saved.getId().toString()));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transaction_rules/bulk - Should archive, restore, and delete rules")
    void handleBulkAction_archiveRestoreDelete() throws Exception {
        BankTransactionRuleEntity r1 = ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Bulk Rule 1")
                .isDeleted(false)
                .build());

        BankTransactionRuleEntity r2 = ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Bulk Rule 2")
                .isDeleted(false)
                .build());

        // Archive
        BankTransactionRuleBulkActionRequest archiveReq = BankTransactionRuleBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(r1.getId(), r2.getId()))
                .build();

        mockMvc.perform(post("/api/v1/bank_transaction_rules/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2));

        mockMvc.perform(get("/api/v1/bank_transaction_rules?status=archived")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Restore
        BankTransactionRuleBulkActionRequest restoreReq = BankTransactionRuleBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(r1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/bank_transaction_rules/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        mockMvc.perform(get("/api/v1/bank_transaction_rules?status=active")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(r1.getId().toString()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Second company cannot view or modify rules")
    void crossTenantIsolation_cannotAccessAnotherCompanyRule() throws Exception {
        BankTransactionRuleEntity rule = ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Secret Financial Rule")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/bank_transaction_rules")
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/bank_transaction_rules/" + rule.getId())
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isNotFound());
    }
}
