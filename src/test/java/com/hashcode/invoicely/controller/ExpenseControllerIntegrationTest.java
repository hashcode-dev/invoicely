package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.procurement.ExpenseBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseDto;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ExpenseControllerIntegrationTest {

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
    private VendorRepository vendorRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SequenceGeneratorService sequenceGeneratorService;

    private final AtomicInteger seqCounter = new AtomicInteger(100);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private VendorEntity testVendor;
    private ClientEntity testClient;
    private ExpenseCategoryEntity testCategory;
    private String jwtToken;

    private CompanyEntity secondCompany;
    private String secondJwtToken;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Operations Corp")
                .subdomain("acme-ops-" + uid)
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Beta Operations Ltd")
                .subdomain("beta-ops-" + uid)
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("ops.controller." + uid + "@invoicely.com")
                .firstName("Operations")
                .lastName("Controller")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .build());

        testVendor = vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("VND-200")
                .name("Fast Delivery Services")
                .currencyId("USD")
                .build());

        testClient = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("CLI-200")
                .name("Acme Enterprise Client")
                .currencyId("USD")
                .balance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .build());

        testCategory = expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Logistics & Shipping")
                .color("#00AAFF")
                .isDeleted(false)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());

        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    @Test
    @DisplayName("GET /api/v1/expenses/create should return default blank expense scaffold")
    void shouldReturnBlankExpenseScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/expenses/create")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.status_id", is("logged")))
                .andExpect(jsonPath("$.data.amount", is(0.0)));
    }

    @Test
    @DisplayName("POST /api/v1/expenses should create expense and compute tax amounts")
    void shouldCreateExpenseWithTaxCalculations() throws Exception {
        ExpenseDto dto = ExpenseDto.builder()
                .vendorId(testVendor.getId())
                .categoryId(testCategory.getId())
                .amount(new BigDecimal("200.00"))
                .date(LocalDate.now().toString())
                .taxName1("GST")
                .taxRate1(new BigDecimal("10.00")) // exclusive 10% of 200 = 20.00
                .usesInclusiveTaxes(false)
                .shouldBeInvoiced(true)
                .publicNotes("Overnight freight delivery")
                .build();

        mockMvc.perform(post("/api/v1/expenses")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.amount", is(200.0)))
                .andExpect(jsonPath("$.data.tax_amount1", is(20.0)))
                .andExpect(jsonPath("$.data.should_be_invoiced", is(true)))
                .andExpect(jsonPath("$.data.category.name", is("Logistics & Shipping")));
    }

    @Test
    @DisplayName("PUT /api/v1/expenses/{id} should update expense and recalculate taxes")
    void shouldUpdateExpense() throws Exception {
        ExpenseEntity entity = expenseRepository.save(ExpenseEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .number("EXP-100")
                .amount(new BigDecimal("100.0000"))
                .taxRate1(new BigDecimal("10.0000"))
                .taxAmount1(new BigDecimal("10.0000"))
                .date(LocalDate.now())
                .statusId("logged")
                .isDeleted(false)
                .build());

        ExpenseDto updateDto = ExpenseDto.builder()
                .amount(new BigDecimal("300.00"))
                .taxRate1(new BigDecimal("10.00"))
                .usesInclusiveTaxes(false)
                .publicNotes("Updated amount")
                .build();

        mockMvc.perform(put("/api/v1/expenses/" + entity.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount", is(300.0)))
                .andExpect(jsonPath("$.data.tax_amount1", is(30.0)));
    }

    @Test
    @DisplayName("POST /api/v1/expenses/bulk with 'bulk_categorize' should categorize expenses")
    void shouldBulkCategorizeExpenses() throws Exception {
        ExpenseCategoryEntity newCategory = expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Office IT")
                .isDeleted(false)
                .build());

        ExpenseEntity exp = expenseRepository.save(ExpenseEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .number("EXP-BULK-1")
                .amount(new BigDecimal("50.0000"))
                .date(LocalDate.now())
                .statusId("logged")
                .isDeleted(false)
                .build());

        ExpenseBulkActionRequest req = ExpenseBulkActionRequest.builder()
                .action("bulk_categorize")
                .ids(List.of(exp.getId()))
                .categoryId(newCategory.getId())
                .build();

        mockMvc.perform(post("/api/v1/expenses/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category.name", is("Office IT")));
    }

    @Test
    @DisplayName("POST /api/v1/expenses/bulk with 'convert_to_invoice' should generate invoice and update client balance")
    void shouldConvertExpensesToInvoice() throws Exception {
        ExpenseEntity exp1 = expenseRepository.save(ExpenseEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .clientId(testClient.getId())
                .number("EXP-INV-1")
                .amount(new BigDecimal("150.0000"))
                .date(LocalDate.now())
                .publicNotes("Consulting expenses")
                .shouldBeInvoiced(true)
                .statusId("logged")
                .isDeleted(false)
                .build());

        ExpenseEntity exp2 = expenseRepository.save(ExpenseEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .clientId(testClient.getId())
                .number("EXP-INV-2")
                .amount(new BigDecimal("250.0000"))
                .date(LocalDate.now())
                .publicNotes("Travel expenses")
                .shouldBeInvoiced(true)
                .statusId("logged")
                .isDeleted(false)
                .build());

        ExpenseBulkActionRequest req = ExpenseBulkActionRequest.builder()
                .action("convert_to_invoice")
                .ids(List.of(exp1.getId(), exp2.getId()))
                .clientId(testClient.getId())
                .build();

        mockMvc.perform(post("/api/v1/expenses/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status_id", is("invoiced")))
                .andExpect(jsonPath("$.data[0].invoice_id", notNullValue()))
                .andExpect(jsonPath("$.data[1].status_id", is("invoiced")))
                .andExpect(jsonPath("$.data[1].invoice_id", notNullValue()));

        ExpenseEntity updatedExp1 = expenseRepository.findById(exp1.getId()).orElseThrow();
        assertNotNull(updatedExp1.getInvoiceId());

        InvoiceEntity invoice = invoiceRepository.findById(updatedExp1.getInvoiceId()).orElseThrow();
        assertEquals(0, invoice.getAmount().compareTo(new BigDecimal("400.0000")));
        assertEquals(0, invoice.getBalance().compareTo(new BigDecimal("400.0000")));
        assertEquals(2, invoice.getLineItems().size());

        ClientEntity updatedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        assertEquals(0, updatedClient.getBalance().compareTo(new BigDecimal("400.0000")));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot access Tenant A expense")
    void shouldEnforceTenantIsolation() throws Exception {
        ExpenseEntity exp = expenseRepository.save(ExpenseEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .number("EXP-SECRET")
                .amount(new BigDecimal("99.0000"))
                .date(LocalDate.now())
                .statusId("logged")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/expenses/" + exp.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
