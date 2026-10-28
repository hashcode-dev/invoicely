package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.banking.*;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BankTransactionControllerIntegrationTest {

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
    private BankTransactionRuleRepository ruleRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private VendorRepository vendorRepository;

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

    private BankIntegrationEntity testBankAccount;
    private VendorEntity testVendor;
    private ExpenseCategoryEntity testCategory;

    @BeforeEach
    void setUp() {
        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> "GEN-" + seqCounter.incrementAndGet());

        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("TxCorp")
                .subdomain("tx-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("treasurer-" + uid + "@txcorp.com")
                .passwordHash("hashedPass")
                .firstName("Treasurer")
                .lastName("Tx")
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

        testBankAccount = bankIntegrationRepository.save(BankIntegrationEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankAccountName("Operations Checking")
                .bankAccountNumber("****5678")
                .bankAccountType("checking")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(new BigDecimal("1000.0000"))
                .isDeleted(false)
                .build());

        testVendor = vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .name("Datadog Monitoring")
                .vendorNumber("VND-2001")
                .build());

        testCategory = expenseCategoryRepository.save(ExpenseCategoryEntity.builder()
                .companyId(testCompany.getId())
                .name("Software Services")
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/bank_transactions/create - Should return blank scaffold")
    void getBlankTransaction_shouldReturnScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/bank_transactions/create")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.base_type").value("debit"))
                .andExpect(jsonPath("$.data.status_id").value("unmatched"))
                .andExpect(jsonPath("$.data.currency_id").value("USD"))
                .andExpect(jsonPath("$.data.amount").value(0))
                .andExpect(jsonPath("$.data.is_deleted").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions - Credit transaction should increase bank account balance")
    void createTransaction_credit_shouldIncreaseAccountBalance() throws Exception {
        BankTransactionDto request = BankTransactionDto.builder()
                .bankIntegrationId(testBankAccount.getId())
                .date(LocalDate.of(2026, 6, 15))
                .amount(new BigDecimal("500.0000"))
                .baseType("credit")
                .description("Customer Wire Payment")
                .participant("Acme Corp")
                .currencyId("USD")
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.amount").value(500.0000))
                .andExpect(jsonPath("$.data.base_type").value("credit"))
                .andExpect(jsonPath("$.data.status_id").value("unmatched"));

        BankIntegrationEntity reloaded = bankIntegrationRepository.findById(testBankAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1500.0000").compareTo(reloaded.getBalance()));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions - Debit transaction should decrease bank account balance")
    void createTransaction_debit_shouldDecreaseAccountBalance() throws Exception {
        BankTransactionDto request = BankTransactionDto.builder()
                .bankIntegrationId(testBankAccount.getId())
                .date(LocalDate.of(2026, 6, 16))
                .amount(new BigDecimal("300.0000"))
                .baseType("debit")
                .description("Office Supplies Purchase")
                .participant("Staples")
                .currencyId("USD")
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.amount").value(300.0000))
                .andExpect(jsonPath("$.data.base_type").value("debit"));

        BankIntegrationEntity reloaded = bankIntegrationRepository.findById(testBankAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("700.0000").compareTo(reloaded.getBalance()));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions - Rule matching should trigger auto-convert to Expense")
    void createTransaction_withMatchingRule_shouldAutoConvert() throws Exception {
        // Create an auto-convert rule matching description contains "Datadog"
        RuleCriteriaDto criteria = RuleCriteriaDto.builder()
                .searchKey("description")
                .operator("contains")
                .value("Datadog")
                .build();

        ruleRepository.save(BankTransactionRuleEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Auto-convert Datadog Rule")
                .appliesTo("all")
                .rules(objectMapper.writeValueAsString(List.of(criteria)))
                .autoConvert(true)
                .matchesOnAll(true)
                .vendorId(testVendor.getId())
                .categoryId(testCategory.getId())
                .isDeleted(false)
                .build());

        BankTransactionDto request = BankTransactionDto.builder()
                .bankIntegrationId(testBankAccount.getId())
                .date(LocalDate.of(2026, 6, 17))
                .amount(new BigDecimal("150.0000"))
                .baseType("debit")
                .description("Datadog Monthly Subscription")
                .participant("Datadog Inc")
                .currencyId("USD")
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status_id").value("converted"))
                .andExpect(jsonPath("$.data.expense_id").isNotEmpty())
                .andExpect(jsonPath("$.data.vendor_id").value(testVendor.getId().toString()))
                .andExpect(jsonPath("$.data.category_id").value(testCategory.getId().toString()));

        // Verify expense created in database
        List<ExpenseEntity> expenses = expenseRepository.findAll();
        assertFalse(expenses.isEmpty());
        ExpenseEntity exp = expenses.get(expenses.size() - 1);
        assertEquals(testVendor.getId(), exp.getVendorId());
        assertEquals(testCategory.getId(), exp.getCategoryId());
        assertEquals(0, new BigDecimal("150.0000").compareTo(exp.getAmount()));
    }

    @Test
    @DisplayName("GET /api/v1/bank_transactions - Filtering by status, base_type, and bank_integration_id")
    void getTransactions_shouldSupportFilteringAndPagination() throws Exception {
        bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankIntegrationId(testBankAccount.getId())
                .date(LocalDate.of(2026, 6, 1))
                .amount(new BigDecimal("200.0000"))
                .baseType("credit")
                .statusId("unmatched")
                .description("Direct Deposit ABC")
                .participant("ABC Corp")
                .isDeleted(false)
                .build());

        bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankIntegrationId(testBankAccount.getId())
                .date(LocalDate.of(2026, 6, 2))
                .amount(new BigDecimal("45.0000"))
                .baseType("debit")
                .statusId("matched")
                .description("Coffee Meeting")
                .participant("Starbucks")
                .isDeleted(false)
                .build());

        // All active transactions
        mockMvc.perform(get("/api/v1/bank_transactions")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total").value(2));

        // Filter by base_type = credit
        mockMvc.perform(get("/api/v1/bank_transactions?base_type=credit")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].description").value("Direct Deposit ABC"));

        // Filter by search string
        mockMvc.perform(get("/api/v1/bank_transactions?filter=Starbucks")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].participant").value("Starbucks"));
    }

    @Test
    @DisplayName("PUT /api/v1/bank_transactions/{id} - Should update transaction")
    void updateTransaction_shouldSucceed() throws Exception {
        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankIntegrationId(testBankAccount.getId())
                .date(LocalDate.of(2026, 6, 5))
                .amount(new BigDecimal("80.0000"))
                .baseType("debit")
                .statusId("unmatched")
                .description("Miscellaneous Purchase")
                .isDeleted(false)
                .build());

        BankTransactionDto updateDto = BankTransactionDto.builder()
                .description("Hardware Store Supplies")
                .participant("Home Depot")
                .vendorId(testVendor.getId())
                .build();

        mockMvc.perform(put("/api/v1/bank_transactions/" + tx.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Hardware Store Supplies"))
                .andExpect(jsonPath("$.data.participant").value("Home Depot"))
                .andExpect(jsonPath("$.data.vendor_id").value(testVendor.getId().toString()));
    }

    @Test
    @DisplayName("DELETE /api/v1/bank_transactions/{id} - Should soft delete transaction")
    void deleteTransaction_shouldSoftDelete() throws Exception {
        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankIntegrationId(testBankAccount.getId())
                .date(LocalDate.of(2026, 6, 5))
                .amount(new BigDecimal("10.0000"))
                .baseType("debit")
                .statusId("unmatched")
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/bank_transactions/" + tx.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bank_transactions?status=deleted")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(tx.getId().toString()));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions/bulk - Should perform archive, restore, and delete")
    void handleBulkAction_archiveRestoreDelete() throws Exception {
        BankTransactionEntity tx1 = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .date(LocalDate.of(2026, 6, 1))
                .amount(new BigDecimal("100.0000"))
                .baseType("credit")
                .statusId("unmatched")
                .isDeleted(false)
                .build());

        BankTransactionEntity tx2 = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .date(LocalDate.of(2026, 6, 2))
                .amount(new BigDecimal("200.0000"))
                .baseType("debit")
                .statusId("unmatched")
                .isDeleted(false)
                .build());

        // Archive
        BankTransactionBulkActionRequest archiveReq = BankTransactionBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(tx1.getId(), tx2.getId()))
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2));

        mockMvc.perform(get("/api/v1/bank_transactions?status=archived")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Restore
        BankTransactionBulkActionRequest restoreReq = BankTransactionBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(tx1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        mockMvc.perform(get("/api/v1/bank_transactions?status=unmatched")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(tx1.getId().toString()));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions/bulk - convert_to_payment should create Payment and mark converted")
    void handleBulkAction_convertToPayment() throws Exception {
        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .date(LocalDate.of(2026, 6, 10))
                .amount(new BigDecimal("750.0000"))
                .baseType("credit")
                .statusId("unmatched")
                .description("Inbound Client Wire")
                .isDeleted(false)
                .build());

        BankTransactionBulkActionRequest req = BankTransactionBulkActionRequest.builder()
                .action("convert_to_payment")
                .ids(List.of(tx.getId()))
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        BankTransactionEntity reloaded = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertEquals("converted", reloaded.getStatusId());
        assertNotNull(reloaded.getPaymentId());

        PaymentEntity payment = paymentRepository.findById(reloaded.getPaymentId()).orElseThrow();
        assertEquals(0, new BigDecimal("750.0000").compareTo(payment.getAmount()));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions/bulk - convert_to_expense should create Expense and mark converted")
    void handleBulkAction_convertToExpense() throws Exception {
        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .vendorId(testVendor.getId())
                .categoryId(testCategory.getId())
                .date(LocalDate.of(2026, 6, 11))
                .amount(new BigDecimal("120.0000"))
                .baseType("debit")
                .statusId("unmatched")
                .description("Server Monitoring")
                .isDeleted(false)
                .build());

        BankTransactionBulkActionRequest req = BankTransactionBulkActionRequest.builder()
                .action("convert_to_expense")
                .ids(List.of(tx.getId()))
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        BankTransactionEntity reloaded = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertEquals("converted", reloaded.getStatusId());
        assertNotNull(reloaded.getExpenseId());

        ExpenseEntity expense = expenseRepository.findById(reloaded.getExpenseId()).orElseThrow();
        assertEquals(0, new BigDecimal("120.0000").compareTo(expense.getAmount()));
        assertEquals(testVendor.getId(), expense.getVendorId());
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions/match - Matching to invoice(s) creates Payment and reduces balance")
    void matchTransactions_toInvoices_shouldCreatePaymentAndClearBalance() throws Exception {
        ClientEntity client = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .name("Global Tech Solutions")
                .clientNumber("CLI-999")
                .balance(new BigDecimal("2000.0000"))
                .paidToDate(BigDecimal.ZERO)
                .build());

        InvoiceEntity invoice = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .clientId(client.getId())
                .number("INV-8888")
                .date(LocalDate.of(2026, 6, 1))
                .dueDate(LocalDate.of(2026, 6, 30))
                .amount(new BigDecimal("2000.0000"))
                .balance(new BigDecimal("2000.0000"))
                .paidToDate(BigDecimal.ZERO)
                .statusId("sent")
                .isDeleted(false)
                .build());

        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .bankIntegrationId(testBankAccount.getId())
                .date(LocalDate.of(2026, 6, 20))
                .amount(new BigDecimal("2000.0000"))
                .baseType("credit")
                .statusId("unmatched")
                .description("Wire from Global Tech Solutions")
                .isDeleted(false)
                .build());

        BankTransactionMatchItem item = BankTransactionMatchItem.builder()
                .id(tx.getId())
                .invoiceIds(invoice.getId().toString())
                .build();

        BankTransactionMatchRequest matchRequest = BankTransactionMatchRequest.builder()
                .transactions(List.of(item))
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions/match")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        // Verify transaction is converted
        BankTransactionEntity reloadedTx = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertEquals("converted", reloadedTx.getStatusId());
        assertNotNull(reloadedTx.getPaymentId());

        // Verify invoice balance is 0 and status is paid
        InvoiceEntity reloadedInv = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(reloadedInv.getBalance()));
        assertEquals(0, new BigDecimal("2000.0000").compareTo(reloadedInv.getPaidToDate()));
        assertEquals("paid", reloadedInv.getStatusId());

        // Verify client balance is reduced
        ClientEntity reloadedClient = clientRepository.findById(client.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(reloadedClient.getBalance()));
        assertEquals(0, new BigDecimal("2000.0000").compareTo(reloadedClient.getPaidToDate()));
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions/match - Matching to existing payment")
    void matchTransactions_toPayment_shouldLinkAndSetStatusMatched() throws Exception {
        ClientEntity payClient = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .name("Direct Pay Client")
                .clientNumber("CLI-PAY-1")
                .build());

        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .clientId(payClient.getId())
                .number("PAY-12345")
                .amount(new BigDecimal("500.0000"))
                .applied(BigDecimal.ZERO)
                .refunded(BigDecimal.ZERO)
                .date(LocalDate.of(2026, 6, 1))
                .typeId("bank_transfer")
                .isDeleted(false)
                .build());

        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .date(LocalDate.of(2026, 6, 1))
                .amount(new BigDecimal("500.0000"))
                .baseType("credit")
                .statusId("unmatched")
                .isDeleted(false)
                .build());

        BankTransactionMatchItem item = BankTransactionMatchItem.builder()
                .id(tx.getId())
                .paymentId(payment.getId().toString())
                .build();

        BankTransactionMatchRequest matchRequest = BankTransactionMatchRequest.builder()
                .transactions(List.of(item))
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions/match")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        BankTransactionEntity reloadedTx = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertEquals("matched", reloadedTx.getStatusId());
        assertEquals(payment.getId(), reloadedTx.getPaymentId());
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions/match - Matching to existing expense")
    void matchTransactions_toExpense_shouldLinkAndSetStatusMatched() throws Exception {
        ExpenseEntity expense = expenseRepository.save(ExpenseEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .number("EXP-12345")
                .amount(new BigDecimal("250.0000"))
                .foreignAmount(new BigDecimal("250.0000"))
                .exchangeRate(BigDecimal.ONE)
                .date(LocalDate.of(2026, 6, 1))
                .isDeleted(false)
                .build());

        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .date(LocalDate.of(2026, 6, 1))
                .amount(new BigDecimal("250.0000"))
                .baseType("debit")
                .statusId("unmatched")
                .isDeleted(false)
                .build());

        BankTransactionMatchItem item = BankTransactionMatchItem.builder()
                .id(tx.getId())
                .expenseId(expense.getId().toString())
                .build();

        BankTransactionMatchRequest matchRequest = BankTransactionMatchRequest.builder()
                .transactions(List.of(item))
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions/match")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        BankTransactionEntity reloadedTx = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertEquals("matched", reloadedTx.getStatusId());
        assertEquals(expense.getId(), reloadedTx.getExpenseId());
    }

    @Test
    @DisplayName("POST /api/v1/bank_transactions/match - Matching to vendor and category creates Expense")
    void matchTransactions_toVendorAndCategory_shouldCreateExpenseAndSetConverted() throws Exception {
        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .date(LocalDate.of(2026, 6, 25))
                .amount(new BigDecimal("350.0000"))
                .baseType("debit")
                .statusId("unmatched")
                .description("Cloud Storage Subscription")
                .isDeleted(false)
                .build());

        BankTransactionMatchItem item = BankTransactionMatchItem.builder()
                .id(tx.getId())
                .vendorId(testVendor.getId().toString())
                .categoryId(testCategory.getId().toString())
                .build();

        BankTransactionMatchRequest matchRequest = BankTransactionMatchRequest.builder()
                .transactions(List.of(item))
                .build();

        mockMvc.perform(post("/api/v1/bank_transactions/match")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        BankTransactionEntity reloadedTx = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertEquals("converted", reloadedTx.getStatusId());
        assertNotNull(reloadedTx.getExpenseId());

        ExpenseEntity expense = expenseRepository.findById(reloadedTx.getExpenseId()).orElseThrow();
        assertEquals(testVendor.getId(), expense.getVendorId());
        assertEquals(testCategory.getId(), expense.getCategoryId());
        assertEquals(0, new BigDecimal("350.0000").compareTo(expense.getAmount()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Second company cannot view or modify transactions")
    void crossTenantIsolation_cannotAccessAnotherCompanyTransaction() throws Exception {
        BankTransactionEntity tx = bankTransactionRepository.save(BankTransactionEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .date(LocalDate.of(2026, 6, 1))
                .amount(new BigDecimal("1000.0000"))
                .baseType("credit")
                .statusId("unmatched")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/bank_transactions")
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/bank_transactions/" + tx.getId())
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isNotFound());
    }
}
