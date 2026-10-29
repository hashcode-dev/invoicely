package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderDto;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderItemDto;
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
public class PurchaseOrderControllerIntegrationTest {

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
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SequenceGeneratorService sequenceGeneratorService;

    private final AtomicInteger seqCounter = new AtomicInteger(100);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private VendorEntity testVendor;
    private String jwtToken;

    private CompanyEntity secondCompany;
    private String secondJwtToken;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Procurement Corp")
                .subdomain("acme-po-" + uid)
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Beta Procurement Ltd")
                .subdomain("beta-po-" + uid)
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("po.agent." + uid + "@invoicely.com")
                .firstName("Procure")
                .lastName("Agent")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .build());

        testVendor = vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("VND-100")
                .name("Global Chip Foundry")
                .currencyId("USD")
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());

        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    @Test
    @DisplayName("GET /api/v1/purchase_orders/create should return default blank PO scaffold")
    void shouldReturnBlankPurchaseOrderScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/purchase_orders/create?vendor_id=" + testVendor.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.vendor_id", is(testVendor.getId().toString())))
                .andExpect(jsonPath("$.data.status_id", is("draft")))
                .andExpect(jsonPath("$.data.line_items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/purchase_orders should create PO and calculate amounts and taxes")
    void shouldCreatePurchaseOrderWithCalculations() throws Exception {
        PurchaseOrderItemDto item1 = PurchaseOrderItemDto.builder()
                .productKey("CHIP-X1")
                .notes("CPU Processor")
                .cost(new BigDecimal("100.00"))
                .quantity(new BigDecimal("5.00"))
                .discount(new BigDecimal("10.00")) // 10% discount -> 500 - 50 = 450
                .isAmountDiscount(false)
                .taxName1("VAT")
                .taxRate1(new BigDecimal("10.00")) // 10% of 450 = 45 -> lineTotal = 495
                .build();

        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .vendorId(testVendor.getId())
                .date(LocalDate.now().toString())
                .dueDate(LocalDate.now().plusDays(15).toString())
                .discount(new BigDecimal("50.00")) // PO level amount discount: 450 - 50 = 400
                .isAmountDiscount(true)
                .lineItems(List.of(item1))
                .build();

        mockMvc.perform(post("/api/v1/purchase_orders")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.vendor_id", is(testVendor.getId().toString())))
                .andExpect(jsonPath("$.data.amount", is(445.0)))
                .andExpect(jsonPath("$.data.balance", is(445.0)))
                .andExpect(jsonPath("$.data.total_taxes", is(45.0)))
                .andExpect(jsonPath("$.data.line_items", hasSize(1)));
    }

    @Test
    @DisplayName("PUT /api/v1/purchase_orders/{id}?mark_sent=true should update status to sent")
    void shouldMarkPurchaseOrderSent() throws Exception {
        PurchaseOrderEntity po = purchaseOrderRepository.save(PurchaseOrderEntity.builder()
                .companyId(testCompany.getId())
                .vendorId(testVendor.getId())
                .number("PO-1001")
                .date(LocalDate.now())
                .statusId("draft")
                .amount(new BigDecimal("200.0000"))
                .balance(new BigDecimal("200.0000"))
                .isDeleted(false)
                .build());

        mockMvc.perform(put("/api/v1/purchase_orders/" + po.getId() + "?mark_sent=true")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status_id", is("sent")));
    }

    @Test
    @DisplayName("POST /api/v1/purchase_orders/bulk with action 'expense' should convert PO to realized Expense")
    void shouldConvertPurchaseOrderToExpense() throws Exception {
        PurchaseOrderEntity po = purchaseOrderRepository.save(PurchaseOrderEntity.builder()
                .companyId(testCompany.getId())
                .vendorId(testVendor.getId())
                .number("PO-CONVERT-01")
                .date(LocalDate.now())
                .statusId("sent")
                .amount(new BigDecimal("750.0000"))
                .balance(new BigDecimal("750.0000"))
                .isDeleted(false)
                .build());

        PurchaseOrderBulkActionRequest req = PurchaseOrderBulkActionRequest.builder()
                .action("expense")
                .ids(List.of(po.getId()))
                .build();

        mockMvc.perform(post("/api/v1/purchase_orders/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status_id", is("converted_to_expense")))
                .andExpect(jsonPath("$.data[0].expense_id", notNullValue()));

        PurchaseOrderEntity updatedPo = purchaseOrderRepository.findById(po.getId()).orElseThrow();
        assertEquals("converted_to_expense", updatedPo.getStatusId());
        assertNotNull(updatedPo.getExpenseId());

        ExpenseEntity createdExp = expenseRepository.findById(updatedPo.getExpenseId()).orElseThrow();
        assertEquals(0, createdExp.getAmount().compareTo(new BigDecimal("750.0000")));
        assertEquals(testVendor.getId(), createdExp.getVendorId());
        assertEquals(po.getId(), createdExp.getPurchaseOrderId());
    }

    @Test
    @DisplayName("POST /api/v1/purchase_orders/bulk should archive and restore POs")
    void shouldBulkArchiveAndRestorePOs() throws Exception {
        PurchaseOrderEntity po = purchaseOrderRepository.save(PurchaseOrderEntity.builder()
                .companyId(testCompany.getId())
                .vendorId(testVendor.getId())
                .number("PO-BULK-01")
                .date(LocalDate.now())
                .statusId("draft")
                .isDeleted(false)
                .build());

        PurchaseOrderBulkActionRequest archiveReq = PurchaseOrderBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(po.getId()))
                .build();

        mockMvc.perform(post("/api/v1/purchase_orders/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].archived_at", notNullValue()));

        PurchaseOrderBulkActionRequest restoreReq = PurchaseOrderBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(po.getId()))
                .build();

        mockMvc.perform(post("/api/v1/purchase_orders/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].archived_at", nullValue()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot access Tenant A purchase order")
    void shouldEnforceTenantIsolation() throws Exception {
        PurchaseOrderEntity po = purchaseOrderRepository.save(PurchaseOrderEntity.builder()
                .companyId(testCompany.getId())
                .vendorId(testVendor.getId())
                .number("PO-SECRET")
                .date(LocalDate.now())
                .statusId("draft")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/purchase_orders/" + po.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
