package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.document.BlueprintBulkActionRequest;
import com.hashcode.invoicely.dto.document.BlueprintDto;
import com.hashcode.invoicely.entity.relational.*;
import com.hashcode.invoicely.repository.relational.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.mock.mockito.MockBean;
import com.hashcode.invoicely.repository.document.InvoicelyOcrRepository;
import com.hashcode.invoicely.entity.document.InvoicelyOcrExtractionDocument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InvoicelyDocsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvoicelyOcrRepository ocrRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyUserRepository companyUserRepository;

    @Autowired
    private BlueprintRepository blueprintRepository;

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
                .companyName("DocsCorp")
                .subdomain("dcs-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("ops-" + uid + "@docscorp.com")
                .passwordHash("hashedPass")
                .firstName("Oliver")
                .lastName("Operations")
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

        when(ocrRepository.save(any(InvoicelyOcrExtractionDocument.class))).thenAnswer(invocation -> {
            InvoicelyOcrExtractionDocument doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID().toString());
            }
            return doc;
        });
    }

    @Test
    @DisplayName("POST /api/client/account_management/v2/invoicely_docs/beta - Should extract AI OCR fields and confidence scores")
    void extractDocumentOcr_shouldParseInvoiceAndReturnConfidence() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "document",
                "scanned_vendor_invoice.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "Vendor: Acme Solutions\nInvoice: INV-123\nTotal: $1375.00".getBytes()
        );

        mockMvc.perform(multipart("/api/client/account_management/v2/invoicely_docs/beta")
                        .file(file)
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.confidence").value(0.98))
                .andExpect(jsonPath("$.extracted_fields.vendor_name").value("Acme Enterprise Solutions"))
                .andExpect(jsonPath("$.extracted_fields.total_amount").value(1375.0))
                .andExpect(jsonPath("$.extracted_fields.currency").value("USD"))
                .andExpect(jsonPath("$.extracted_fields.line_items", hasSize(2)));
    }

    @Test
    @DisplayName("POST /api/documents/ocr - Alternate OCR endpoint should also extract successfully")
    void extractDocumentOcr_alternateEndpoint_shouldSucceed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vendor_receipt.png",
                MediaType.IMAGE_PNG_VALUE,
                "Receipt Data".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/ocr")
                        .file(file)
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.extracted_fields.vendor_name").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/documents/{id}/timeline - Should return lifecycle tracking events")
    void getDocumentTimeline_shouldReturnLifecycleEvents() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(post("/api/documents/" + docId + "/timeline")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Document Created"))
                .andExpect(jsonPath("$[1].title").value("Document Viewed"));
    }

    @Test
    @DisplayName("POST /api/blueprints - Should create blueprint for document signing")
    void createBlueprint_shouldSucceed() throws Exception {
        BlueprintDto request = BlueprintDto.builder()
                .name("Non-Disclosure Agreement")
                .content("Standard mutual NDA agreement clauses...")
                .isTemplate(true)
                .build();

        mockMvc.perform(post("/api/blueprints")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Non-Disclosure Agreement"))
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.is_template").value(true));
    }

    @Test
    @DisplayName("GET /api/blueprints - Should list blueprints with filtering")
    void getBlueprints_shouldReturnPagedList() throws Exception {
        blueprintRepository.save(BlueprintEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Sales Agreement Template")
                .status("draft")
                .isDeleted(false)
                .build());

        blueprintRepository.save(BlueprintEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Employment Contract")
                .status("draft")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/blueprints")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total").value(2));

        mockMvc.perform(get("/api/blueprints?filter=Sales")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Sales Agreement Template"));
    }

    @Test
    @DisplayName("GET /api/blueprints/{id} - Should return blueprint details")
    void getBlueprintById_shouldReturnDetails() throws Exception {
        BlueprintEntity saved = blueprintRepository.save(BlueprintEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Consulting Contract")
                .status("draft")
                .content("Clause 1: Scope of work")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/blueprints/" + saved.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Consulting Contract"));
    }

    @Test
    @DisplayName("POST /api/blueprints/{id}/send - Should mark blueprint as sent")
    void sendBlueprint_shouldUpdateStatusToSent() throws Exception {
        BlueprintEntity saved = blueprintRepository.save(BlueprintEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Signable Agreement")
                .status("draft")
                .isDeleted(false)
                .build());

        mockMvc.perform(post("/api/blueprints/" + saved.getId() + "/send")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("sent"));
    }

    @Test
    @DisplayName("POST /api/blueprints/bulk - Should archive, restore, and delete blueprints")
    void handleBulkAction_archiveRestoreDelete() throws Exception {
        BlueprintEntity b1 = blueprintRepository.save(BlueprintEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Bulk BP 1")
                .isDeleted(false)
                .build());

        BlueprintEntity b2 = blueprintRepository.save(BlueprintEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Bulk BP 2")
                .isDeleted(false)
                .build());

        // Archive
        BlueprintBulkActionRequest archiveReq = BlueprintBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(b1.getId(), b2.getId()))
                .build();

        mockMvc.perform(post("/api/blueprints/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2));

        mockMvc.perform(get("/api/blueprints?status=archived")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Restore
        BlueprintBulkActionRequest restoreReq = BlueprintBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(b1.getId()))
                .build();

        mockMvc.perform(post("/api/blueprints/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        mockMvc.perform(get("/api/blueprints?status=active")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(b1.getId().toString()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Second company cannot view or modify blueprints")
    void crossTenantIsolation_cannotAccessAnotherCompanyBlueprint() throws Exception {
        BlueprintEntity bp = blueprintRepository.save(BlueprintEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Secret Merger Blueprint")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/blueprints")
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/blueprints/" + bp.getId())
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isNotFound());
    }
}
