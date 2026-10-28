package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.document.TemplateRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TemplateControllerIntegrationTest {

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

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private ClientEntity testClient;
    private InvoiceEntity testInvoice;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("TemplateMaster Inc")
                .subdomain("tpl-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("admin-" + uid + "@templatemaster.com")
                .passwordHash("hashedPass")
                .firstName("Tom")
                .lastName("Template")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .build());

        testClient = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .name("Acme Corporation")
                .clientNumber("CLI-1001")
                .build());

        testInvoice = invoiceRepository.save(InvoiceEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .clientId(testClient.getId())
                .number("INV-9999")
                .date(LocalDate.of(2026, 7, 1))
                .dueDate(LocalDate.of(2026, 7, 31))
                .amount(new BigDecimal("1500.0000"))
                .balance(new BigDecimal("1500.0000"))
                .statusId("sent")
                .isDeleted(false)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("POST /api/v1/templates - Should interpolate template variables from invoice and company context")
    void renderTemplate_withInvoiceData_shouldInterpolateVariables() throws Exception {
        TemplateRequest request = TemplateRequest.builder()
                .subject("New invoice $invoice.number from $company.name")
                .body("Hello $client.name, your invoice for $amount is due on $due_date.")
                .entity("invoice")
                .entityId(testInvoice.getId().toString())
                .template("email_template_invoice")
                .build();

        mockMvc.perform(post("/api/v1/templates")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("New invoice INV-9999 from TemplateMaster Inc"))
                .andExpect(jsonPath("$.data.body").value("Hello Acme Corporation, your invoice for $1500.0000 is due on 2026-07-31."))
                .andExpect(jsonPath("$.data.raw_body").value("Hello $client.name, your invoice for $amount is due on $due_date."));
    }

    @Test
    @DisplayName("POST /api/v1/templates/preview/{hash} - Should generate binary PDF preview stream")
    void renderPreview_shouldReturnPdfByteArray() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/templates/preview/token123456")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 50);
        String header = new String(pdfBytes, 0, 5);
        assertTrue(header.startsWith("%PDF-"));
    }
}
