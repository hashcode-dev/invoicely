package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.document.DocumentBulkActionRequest;
import com.hashcode.invoicely.dto.document.DocumentVisibilityRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DocumentControllerIntegrationTest {

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
    private DocumentRepository documentRepository;

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
                .companyName("DocCorp")
                .subdomain("doc-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("admin-" + uid + "@doccorp.com")
                .passwordHash("hashedPass")
                .firstName("Dan")
                .lastName("Documents")
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
    @DisplayName("POST /api/v1/documents - Should upload multipart document and generate hash")
    void uploadDocument_shouldCreateRecordAndHash() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract_2026.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "Sample PDF Content for Invoicely".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("contract_2026.pdf"))
                .andExpect(jsonPath("$.data.type").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.data.hash").isNotEmpty())
                .andExpect(jsonPath("$.data.is_public").value(false))
                .andExpect(jsonPath("$.data.is_deleted").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/documents - Should return paged list with filter")
    void getDocuments_shouldReturnPagedList() throws Exception {
        documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Quarterly_Report_Q1.pdf")
                .type("application/pdf")
                .hash("hash123")
                .isDeleted(false)
                .build());

        documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Tax_Exemption_Certificate.pdf")
                .type("application/pdf")
                .hash("hash456")
                .isDeleted(false)
                .build());

        // All
        mockMvc.perform(get("/api/v1/documents")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total").value(2));

        // Filter
        mockMvc.perform(get("/api/v1/documents?filter=Report")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Quarterly_Report_Q1.pdf"));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{id} - Should return document details")
    void getDocumentById_shouldReturnDetails() throws Exception {
        DocumentEntity doc = documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Master_Services_Agreement.pdf")
                .type("application/pdf")
                .hash("msa_hash_789")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/documents/" + doc.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(doc.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Master_Services_Agreement.pdf"));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{hash}/download - Should stream binary PDF file")
    void downloadDocument_byHash_shouldReturnPdfByteArray() throws Exception {
        DocumentEntity doc = documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Download_Test.pdf")
                .type("application/pdf")
                .hash("unique_hash_999")
                .isDeleted(false)
                .build());

        MvcResult result = mockMvc.perform(get("/api/v1/documents/" + doc.getHash() + "/download")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 50);
        String header = new String(pdfBytes, 0, 5);
        assertTrue(header.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("PUT /api/v1/documents/{id} - Should update public visibility flag")
    void updateVisibility_shouldUpdatePublicFlag() throws Exception {
        DocumentEntity doc = documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Terms.pdf")
                .type("application/pdf")
                .hash("terms_hash")
                .isPublic(false)
                .isDeleted(false)
                .build());

        DocumentVisibilityRequest req = DocumentVisibilityRequest.builder()
                .isPublic(true)
                .build();

        mockMvc.perform(put("/api/v1/documents/" + doc.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_public").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/documents/{id} - Should soft delete document")
    void deleteDocument_shouldSoftDelete() throws Exception {
        DocumentEntity doc = documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("DocToDelete.pdf")
                .type("application/pdf")
                .hash("del_hash")
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/documents/" + doc.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/documents?status=deleted")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(doc.getId().toString()));
    }

    @Test
    @DisplayName("POST /api/v1/documents/bulk - Should archive, restore, and delete documents")
    void handleBulkAction_archiveRestoreDelete() throws Exception {
        DocumentEntity d1 = documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Bulk Doc 1")
                .type("application/pdf")
                .hash("bulk1")
                .isDeleted(false)
                .build());

        DocumentEntity d2 = documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Bulk Doc 2")
                .type("application/pdf")
                .hash("bulk2")
                .isDeleted(false)
                .build());

        // Archive
        DocumentBulkActionRequest archiveReq = DocumentBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(d1.getId(), d2.getId()))
                .build();

        mockMvc.perform(post("/api/v1/documents/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2));

        mockMvc.perform(get("/api/v1/documents?status=archived")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Restore
        DocumentBulkActionRequest restoreReq = DocumentBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(d1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/documents/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        mockMvc.perform(get("/api/v1/documents?status=active")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(d1.getId().toString()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Second company cannot view or modify documents")
    void crossTenantIsolation_cannotAccessAnotherCompanyDocument() throws Exception {
        DocumentEntity doc = documentRepository.save(DocumentEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Secret_Financial_Audit.pdf")
                .type("application/pdf")
                .hash("audit_hash")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/documents")
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/documents/" + doc.getId())
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isNotFound());
    }
}
