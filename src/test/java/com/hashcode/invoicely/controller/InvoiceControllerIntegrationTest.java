package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.invoice.InvoiceBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.invoice.InvoiceItemDto;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
class InvoiceControllerIntegrationTest {

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
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SequenceGeneratorService sequenceGeneratorService;

    private final AtomicInteger seqCounter = new AtomicInteger(500);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private ClientEntity testClient;
    private String jwtToken;

    private CompanyEntity secondCompany;
    private String secondJwtToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Billing Inc")
                .subdomain("acme-billing")
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Gamma LLC")
                .subdomain("gamma-billing")
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("invoicing.lead@invoicely.com")
                .firstName("Invoice")
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
                .clientNumber("C001")
                .name("Global Megacorp")
                .currencyId("USD")
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());

        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/create should return default blank invoice with sequence number")
    void shouldReturnBlankInvoice() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/create?client_id=" + testClient.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.client_id", is(testClient.getId().toString())))
                .andExpect(jsonPath("$.data.status_id", is("draft")))
                .andExpect(jsonPath("$.data.line_items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/invoices should create invoice, perform financial calculation, and update client balance")
    void shouldCreateInvoiceAndCalculate() throws Exception {
        InvoiceItemDto item1 = InvoiceItemDto.builder()
                .productKey("SRV-CONSULT")
                .notes("Consulting Services 10h")
                .quantity(new BigDecimal("10.0000"))
                .cost(new BigDecimal("100.0000"))
                .discount(new BigDecimal("10.0000")) // 10% discount -> net 900
                .isAmountDiscount(false)
                .taxName1("VAT")
                .taxRate1(new BigDecimal("10.0000")) // 10% tax -> tax 90, gross 990
                .build();

        InvoiceDto request = InvoiceDto.builder()
                .clientId(testClient.getId())
                .date("2026-10-28")
                .dueDate("2026-11-28")
                .terms("Net 30 days")
                .lineItems(Collections.singletonList(item1))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.amount", is(990.0)))
                .andExpect(jsonPath("$.data.balance", is(990.0)))
                .andExpect(jsonPath("$.data.total_taxes", is(90.0)))
                .andExpect(jsonPath("$.data.line_items", hasSize(1)))
                .andExpect(jsonPath("$.data.line_items[0].line_total", is(900.0)))
                .andExpect(jsonPath("$.data.line_items[0].gross_line_total", is(990.0)));

        // Verify client balance was increased
        ClientEntity reloadedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("990.0000").compareTo(reloadedClient.getBalance()));
    }

    @Test
    @DisplayName("GET /api/v1/invoices should return paginated list and filter by status and number")
    void shouldReturnPaginatedInvoices() throws Exception {
        InvoiceEntity inv1 = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-0001")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("500.0000"))
                .balance(new BigDecimal("500.0000"))
                .statusId("draft")
                .build());

        InvoiceEntity inv2 = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-0002")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("1200.0000"))
                .balance(new BigDecimal("1200.0000"))
                .statusId("sent")
                .build());

        // Without filter
        mockMvc.perform(get("/api/v1/invoices")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total", is(2)));

        // Filter by status=sent
        mockMvc.perform(get("/api/v1/invoices?status=sent")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].number", is("INV-0002")));

        // Multi-tenant check: second company sees 0 invoices
        mockMvc.perform(get("/api/v1/invoices")
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/{id} should return invoice details and 404 for cross-tenant")
    void shouldGetInvoiceById() throws Exception {
        InvoiceEntity inv = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-0010")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("750.0000"))
                .balance(new BigDecimal("750.0000"))
                .build());

        mockMvc.perform(get("/api/v1/invoices/" + inv.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(inv.getId().toString())))
                .andExpect(jsonPath("$.data.number", is("INV-0010")));

        // Cross-tenant access
        mockMvc.perform(get("/api/v1/invoices/" + inv.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/invoices/{id} should update line items and recalculate totals")
    void shouldUpdateInvoice() throws Exception {
        InvoiceEntity inv = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-0020")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("100.0000"))
                .balance(new BigDecimal("100.0000"))
                .build());

        InvoiceItemDto newItem = InvoiceItemDto.builder()
                .productKey("NEW-PROD")
                .quantity(new BigDecimal("2.0000"))
                .cost(new BigDecimal("300.0000"))
                .discount(BigDecimal.ZERO)
                .build();

        InvoiceDto updateReq = InvoiceDto.builder()
                .terms("Updated payment terms")
                .lineItems(Collections.singletonList(newItem))
                .build();

        mockMvc.perform(put("/api/v1/invoices/" + inv.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount", is(600.0)))
                .andExpect(jsonPath("$.data.balance", is(600.0)))
                .andExpect(jsonPath("$.data.terms", is("Updated payment terms")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/mark_sent and /mark_paid lifecycle transitions")
    void shouldHandleLifecycleTransitions() throws Exception {
        InvoiceEntity inv = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-0030")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("450.0000"))
                .balance(new BigDecimal("450.0000"))
                .statusId("draft")
                .build());

        // Mark Sent
        mockMvc.perform(post("/api/v1/invoices/" + inv.getId() + "/mark_sent")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status_id", is("sent")));

        // Mark Paid
        mockMvc.perform(post("/api/v1/invoices/" + inv.getId() + "/mark_paid")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status_id", is("paid")))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.paid_to_date", is(450.0)));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/clone should duplicate invoice with next sequence")
    void shouldCloneInvoice() throws Exception {
        InvoiceEntity inv = InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-0040")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("200.0000"))
                .balance(new BigDecimal("200.0000"))
                .statusId("draft")
                .build();
        inv.addLineItem(InvoiceItemEntity.builder()
                .productKey("PROD-1")
                .quantity(BigDecimal.ONE)
                .cost(new BigDecimal("200.0000"))
                .lineTotal(new BigDecimal("200.0000"))
                .grossLineTotal(new BigDecimal("200.0000"))
                .build());
        InvoiceEntity saved = invoiceRepository.save(inv);

        mockMvc.perform(post("/api/v1/invoices/" + saved.getId() + "/clone")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.amount", is(200.0)))
                .andExpect(jsonPath("$.data.line_items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/bulk should execute bulk operations")
    void shouldExecuteBulkOperations() throws Exception {
        InvoiceEntity inv1 = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-B1")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("100.0000"))
                .balance(new BigDecimal("100.0000"))
                .build());

        InvoiceEntity inv2 = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-B2")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("200.0000"))
                .balance(new BigDecimal("200.0000"))
                .build());

        List<UUID> ids = Arrays.asList(inv1.getId(), inv2.getId());

        // Bulk archive
        InvoiceBulkActionRequest archiveReq = InvoiceBulkActionRequest.builder()
                .action("archive")
                .ids(ids)
                .build();

        mockMvc.perform(post("/api/v1/invoices/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].archived_at", notNullValue()));

        // Bulk restore
        InvoiceBulkActionRequest restoreReq = InvoiceBulkActionRequest.builder()
                .action("restore")
                .ids(ids)
                .build();

        mockMvc.perform(post("/api/v1/invoices/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].archived_at", nullValue()));
    }
}
