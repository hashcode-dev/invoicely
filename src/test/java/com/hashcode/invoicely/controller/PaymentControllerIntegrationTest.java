package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.payment.*;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PaymentControllerIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentableRepository paymentableRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SequenceGeneratorService sequenceGeneratorService;

    private final AtomicInteger seqCounter = new AtomicInteger(100);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private ClientEntity testClient;
    private String jwtToken;

    private CompanyEntity secondCompany;
    private String secondJwtToken;

    @BeforeEach
    void setUp() {
        String uid = java.util.UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Payments Global")
                .subdomain("acme-payments-" + uid)
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Beta Invoicing Ltd")
                .subdomain("beta-invoicing-" + uid)
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("ledger." + uid + "@invoicely.com")
                .firstName("Ledger")
                .lastName("Director")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .build());

        testClient = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("CLI-100")
                .name("Starlight Technologies")
                .currencyId("USD")
                .balance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .paidToDate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .creditBalance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());

        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    private InvoiceEntity createTestInvoice(BigDecimal amount, BigDecimal balance) {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("INV-" + seqCounter.incrementAndGet())
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .statusId("sent")
                .amount(amount.setScale(4, RoundingMode.HALF_UP))
                .balance(balance.setScale(4, RoundingMode.HALF_UP))
                .paidToDate(amount.subtract(balance).setScale(4, RoundingMode.HALF_UP))
                .isDeleted(false)
                .build();
        return invoiceRepository.save(invoice);
    }

    @Test
    @DisplayName("GET /api/v1/payments/create should return default blank payment scaffold")
    void shouldReturnBlankPaymentScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/payments/create?client_id=" + testClient.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.client_id", is(testClient.getId().toString())))
                .andExpect(jsonPath("$.data.status_id", is("1")))
                .andExpect(jsonPath("$.data.amount", is(0.0)))
                .andExpect(jsonPath("$.data.applied", is(0.0)));
    }

    @Test
    @DisplayName("POST /api/v1/payments should fully allocate against invoice and mark invoice paid")
    void shouldCreatePaymentAndFullyAllocateInvoice() throws Exception {
        InvoiceEntity invoice = createTestInvoice(new BigDecimal("500.00"), new BigDecimal("500.00"));
        testClient.setBalance(new BigDecimal("500.0000"));
        clientRepository.save(testClient);

        CreatePaymentRequest req = CreatePaymentRequest.builder()
                .clientId(testClient.getId())
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now().toString())
                .typeId("1")
                .transactionReference("TXN-PAY-001")
                .invoices(List.of(
                        PaymentInvoiceAllocationRequest.builder()
                                .invoiceId(invoice.getId())
                                .amount(new BigDecimal("500.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.amount", is(500.0)))
                .andExpect(jsonPath("$.data.applied", is(500.0)))
                .andExpect(jsonPath("$.data.status_id", is("completed")))
                .andExpect(jsonPath("$.data.paymentables", hasSize(1)));

        // Verify Invoice state
        InvoiceEntity updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(0, updatedInvoice.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, updatedInvoice.getPaidToDate().compareTo(new BigDecimal("500.0000")));
        assertEquals("paid", updatedInvoice.getStatusId());

        // Verify Client ledger state
        ClientEntity updatedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        assertEquals(0, updatedClient.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, updatedClient.getPaidToDate().compareTo(new BigDecimal("500.0000")));
    }

    @Test
    @DisplayName("POST /api/v1/payments should partially allocate against invoice and mark invoice partial")
    void shouldCreatePaymentAndPartiallyAllocateInvoice() throws Exception {
        InvoiceEntity invoice = createTestInvoice(new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        testClient.setBalance(new BigDecimal("1000.0000"));
        clientRepository.save(testClient);

        CreatePaymentRequest req = CreatePaymentRequest.builder()
                .clientId(testClient.getId())
                .amount(new BigDecimal("400.00"))
                .date(LocalDate.now().toString())
                .typeId("1")
                .invoices(List.of(
                        PaymentInvoiceAllocationRequest.builder()
                                .invoiceId(invoice.getId())
                                .amount(new BigDecimal("400.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount", is(400.0)))
                .andExpect(jsonPath("$.data.applied", is(400.0)));

        InvoiceEntity updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(0, updatedInvoice.getBalance().compareTo(new BigDecimal("600.0000")));
        assertEquals(0, updatedInvoice.getPaidToDate().compareTo(new BigDecimal("400.0000")));
        assertEquals("partial", updatedInvoice.getStatusId());

        ClientEntity updatedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        assertEquals(0, updatedClient.getBalance().compareTo(new BigDecimal("600.0000")));
        assertEquals(0, updatedClient.getPaidToDate().compareTo(new BigDecimal("400.0000")));
    }

    @Test
    @DisplayName("POST /api/v1/payments should support multi-invoice allocation")
    void shouldSupportMultiInvoiceAllocation() throws Exception {
        InvoiceEntity inv1 = createTestInvoice(new BigDecimal("200.00"), new BigDecimal("200.00"));
        InvoiceEntity inv2 = createTestInvoice(new BigDecimal("300.00"), new BigDecimal("300.00"));
        testClient.setBalance(new BigDecimal("500.0000"));
        clientRepository.save(testClient);

        CreatePaymentRequest req = CreatePaymentRequest.builder()
                .clientId(testClient.getId())
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now().toString())
                .invoices(List.of(
                        PaymentInvoiceAllocationRequest.builder().invoiceId(inv1.getId()).amount(new BigDecimal("200.00")).build(),
                        PaymentInvoiceAllocationRequest.builder().invoiceId(inv2.getId()).amount(new BigDecimal("300.00")).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.applied", is(500.0)))
                .andExpect(jsonPath("$.data.paymentables", hasSize(2)));

        InvoiceEntity updatedInv1 = invoiceRepository.findById(inv1.getId()).orElseThrow();
        assertEquals(0, updatedInv1.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals("paid", updatedInv1.getStatusId());

        InvoiceEntity updatedInv2 = invoiceRepository.findById(inv2.getId()).orElseThrow();
        assertEquals(0, updatedInv2.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals("paid", updatedInv2.getStatusId());

        ClientEntity updatedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        assertEquals(0, updatedClient.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, updatedClient.getPaidToDate().compareTo(new BigDecimal("500.0000")));
    }

    @Test
    @DisplayName("POST /api/v1/payments overpayment should deposit excess into client credit balance")
    void shouldHandleOverpaymentIntoClientCreditBalance() throws Exception {
        InvoiceEntity invoice = createTestInvoice(new BigDecimal("300.00"), new BigDecimal("300.00"));
        testClient.setBalance(new BigDecimal("300.0000"));
        testClient.setCreditBalance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        clientRepository.save(testClient);

        // Payment is 500, but only 300 allocated to invoice
        CreatePaymentRequest req = CreatePaymentRequest.builder()
                .clientId(testClient.getId())
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now().toString())
                .invoices(List.of(
                        PaymentInvoiceAllocationRequest.builder().invoiceId(invoice.getId()).amount(new BigDecimal("300.00")).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount", is(500.0)))
                .andExpect(jsonPath("$.data.applied", is(300.0)));

        InvoiceEntity updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(0, updatedInvoice.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals("paid", updatedInvoice.getStatusId());

        ClientEntity updatedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        assertEquals(0, updatedClient.getBalance().compareTo(BigDecimal.ZERO));
        // Credit balance should have received the unapplied excess of 200
        assertEquals(0, updatedClient.getCreditBalance().compareTo(new BigDecimal("200.0000")));
        assertEquals(0, updatedClient.getPaidToDate().compareTo(new BigDecimal("500.0000")));
    }

    @Test
    @DisplayName("POST /api/v1/payments/{id}/refund should reverse allocation, restore invoice balance, and update client ledger")
    void shouldRefundPaymentAndReverseLedger() throws Exception {
        InvoiceEntity invoice = createTestInvoice(new BigDecimal("500.00"), new BigDecimal("500.00"));
        testClient.setBalance(new BigDecimal("500.0000"));
        clientRepository.save(testClient);

        CreatePaymentRequest payReq = CreatePaymentRequest.builder()
                .clientId(testClient.getId())
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now().toString())
                .invoices(List.of(
                        PaymentInvoiceAllocationRequest.builder().invoiceId(invoice.getId()).amount(new BigDecimal("500.00")).build()
                ))
                .build();

        String paymentRespStr = mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        PaymentDto createdPayment = objectMapper.readTree(paymentRespStr).get("data").traverse(objectMapper).readValueAs(PaymentDto.class);

        // Perform partial refund of 200
        PaymentRefundRequest refundReq = PaymentRefundRequest.builder()
                .id(createdPayment.getId())
                .date(LocalDate.now().toString())
                .invoices(List.of(
                        PaymentInvoiceAllocationRequest.builder().invoiceId(invoice.getId()).amount(new BigDecimal("200.00")).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/payments/" + createdPayment.getId() + "/refund")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refunded", is(200.0)))
                .andExpect(jsonPath("$.data.status_id", is("partially_refunded")));

        // Verify Invoice balance is restored by 200 and status becomes "partial"
        InvoiceEntity refundedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(0, refundedInvoice.getBalance().compareTo(new BigDecimal("200.0000")));
        assertEquals(0, refundedInvoice.getPaidToDate().compareTo(new BigDecimal("300.0000")));
        assertEquals("partial", refundedInvoice.getStatusId());

        // Verify Client balance is restored by 200
        ClientEntity refundedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        assertEquals(0, refundedClient.getBalance().compareTo(new BigDecimal("200.0000")));
        assertEquals(0, refundedClient.getPaidToDate().compareTo(new BigDecimal("300.0000")));

        // Now perform full refund of remaining 300 via POST /api/v1/payments/refund
        PaymentRefundRequest fullRefundReq = PaymentRefundRequest.builder()
                .id(createdPayment.getId())
                .date(LocalDate.now().toString())
                .invoices(List.of(
                        PaymentInvoiceAllocationRequest.builder().invoiceId(invoice.getId()).amount(new BigDecimal("300.00")).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/payments/refund")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullRefundReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refunded", is(500.0)))
                .andExpect(jsonPath("$.data.status_id", is("refunded")));

        InvoiceEntity fullyRefundedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(0, fullyRefundedInvoice.getBalance().compareTo(new BigDecimal("500.0000")));
        assertEquals(0, fullyRefundedInvoice.getPaidToDate().compareTo(BigDecimal.ZERO));
        assertEquals("sent", fullyRefundedInvoice.getStatusId());

        ClientEntity fullyRefundedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        assertEquals(0, fullyRefundedClient.getBalance().compareTo(new BigDecimal("500.0000")));
        assertEquals(0, fullyRefundedClient.getPaidToDate().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("POST /api/v1/payments/bulk should archive and restore payments")
    void shouldBulkArchiveAndRestorePayments() throws Exception {
        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("PAY-BULK-001")
                .amount(new BigDecimal("100.0000"))
                .date(LocalDate.now())
                .typeId("1")
                .statusId("completed")
                .isDeleted(false)
                .build());

        PaymentBulkActionRequest archiveReq = PaymentBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(payment.getId()))
                .build();

        mockMvc.perform(post("/api/v1/payments/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].archived_at", notNullValue()));

        PaymentBulkActionRequest restoreReq = PaymentBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(payment.getId()))
                .build();

        mockMvc.perform(post("/api/v1/payments/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].archived_at", nullValue()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot access Tenant A payment")
    void shouldEnforceCrossTenantIsolation() throws Exception {
        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("PAY-TENANT-A")
                .amount(new BigDecimal("150.0000"))
                .date(LocalDate.now())
                .typeId("1")
                .statusId("completed")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/payments/" + payment.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
