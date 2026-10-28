package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.invoice.QuoteBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.QuoteDto;
import com.hashcode.invoicely.dto.invoice.QuoteItemDto;
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
import java.util.Collections;
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
class QuoteControllerIntegrationTest {

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
    private QuoteRepository quoteRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SequenceGeneratorService sequenceGeneratorService;

    private final AtomicInteger seqCounter = new AtomicInteger(700);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private ClientEntity testClient;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Quotes")
                .subdomain("acme-quotes")
                .currencyId("USD")
                .countryId("US")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("quotes.lead@invoicely.com")
                .firstName("Quote")
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
                .clientNumber("C002")
                .name("Potential Client LLC")
                .currencyId("USD")
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());

        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    @Test
    @DisplayName("GET /api/v1/quotes/create should return blank quote defaults")
    void shouldReturnBlankQuote() throws Exception {
        mockMvc.perform(get("/api/v1/quotes/create?client_id=" + testClient.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.status_id", is("draft")))
                .andExpect(jsonPath("$.data.line_items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/quotes should create a new estimate/quote")
    void shouldCreateQuote() throws Exception {
        QuoteItemDto item = QuoteItemDto.builder()
                .productKey("DES-PROJ")
                .notes("Website Design Project")
                .quantity(new BigDecimal("1.0000"))
                .cost(new BigDecimal("1500.0000"))
                .build();

        QuoteDto request = QuoteDto.builder()
                .clientId(testClient.getId())
                .date("2026-10-28")
                .dueDate("2026-11-15")
                .terms("Valid for 30 days")
                .lineItems(Collections.singletonList(item))
                .build();

        mockMvc.perform(post("/api/v1/quotes")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.amount", is(1500.0)))
                .andExpect(jsonPath("$.data.line_items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/quotes/{id}/convert_to_invoice should convert estimate to active invoice")
    void shouldConvertQuoteToInvoice() throws Exception {
        QuoteEntity quote = QuoteEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("Q-001")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .amount(new BigDecimal("2500.0000"))
                .balance(new BigDecimal("2500.0000"))
                .statusId("approved")
                .build();
        quote.addLineItem(QuoteItemEntity.builder()
                .productKey("ENG-BUILD")
                .quantity(BigDecimal.ONE)
                .cost(new BigDecimal("2500.0000"))
                .lineTotal(new BigDecimal("2500.0000"))
                .build());
        QuoteEntity savedQuote = quoteRepository.save(quote);

        mockMvc.perform(post("/api/v1/quotes/" + savedQuote.getId() + "/convert_to_invoice")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.amount", is(2500.0)))
                .andExpect(jsonPath("$.data.status_id", is("draft")));

        // Verify quote status changed to converted
        QuoteEntity reloadedQuote = quoteRepository.findById(savedQuote.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("converted", reloadedQuote.getStatusId());
        org.junit.jupiter.api.Assertions.assertNotNull(reloadedQuote.getInvoiceId());

        // Verify client balance increased
        ClientEntity reloadedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("2500.0000"), reloadedClient.getBalance());
    }

    @Test
    @DisplayName("POST /api/v1/quotes/bulk should handle approve and archive actions")
    void shouldHandleBulkActions() throws Exception {
        QuoteEntity q1 = quoteRepository.save(QuoteEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("Q-B1")
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .amount(new BigDecimal("300.0000"))
                .balance(new BigDecimal("300.0000"))
                .statusId("draft")
                .build());

        QuoteBulkActionRequest request = QuoteBulkActionRequest.builder()
                .action("approve")
                .ids(Collections.singletonList(q1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/quotes/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status_id", is("approved")));
    }
}
