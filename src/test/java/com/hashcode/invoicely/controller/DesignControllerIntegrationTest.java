package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.document.DesignBulkActionRequest;
import com.hashcode.invoicely.dto.document.DesignDto;
import com.hashcode.invoicely.dto.document.PartsDto;
import com.hashcode.invoicely.dto.document.SetDefaultDesignRequest;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DesignControllerIntegrationTest {

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
    private DesignRepository designRepository;

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
                .companyName("DesignCorp")
                .subdomain("dsg-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("designer-" + uid + "@designcorp.com")
                .passwordHash("hashedPass")
                .firstName("Diane")
                .lastName("Designer")
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
    @DisplayName("GET /api/v1/designs/create - Should return blank design scaffold")
    void getBlankDesign_shouldReturnScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/designs/create")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Custom Design"))
                .andExpect(jsonPath("$.data.is_custom").value(true))
                .andExpect(jsonPath("$.data.is_active").value(true))
                .andExpect(jsonPath("$.data.is_deleted").value(false))
                .andExpect(jsonPath("$.data.design.header").isNotEmpty())
                .andExpect(jsonPath("$.data.design.body").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/designs - Should create custom design")
    void createDesign_shouldSucceed() throws Exception {
        PartsDto parts = PartsDto.builder()
                .header("<header><h1>Modern Brand</h1></header>")
                .body("<main><p>Invoice Details</p></main>")
                .product("<table><tr><td>Item</td></tr></table>")
                .task("<table><tr><td>Task</td></tr></table>")
                .footer("<footer>Page 1 of 1</footer>")
                .includes("<style>.header { color: #004bca; }</style>")
                .build();

        DesignDto request = DesignDto.builder()
                .name("Corporate Sleek")
                .design(parts)
                .isCustom(true)
                .isActive(true)
                .entities("invoice")
                .build();

        mockMvc.perform(post("/api/v1/designs")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Corporate Sleek"))
                .andExpect(jsonPath("$.data.is_custom").value(true))
                .andExpect(jsonPath("$.data.design.header").value("<header><h1>Modern Brand</h1></header>"));
    }

    @Test
    @DisplayName("GET /api/v1/designs - Should return paged list with filter")
    void getDesigns_shouldReturnPagedListWithFilters() throws Exception {
        designRepository.save(DesignEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Alpha Template")
                .design("{}")
                .isCustom(true)
                .isActive(true)
                .isDeleted(false)
                .entities("invoice")
                .build());

        designRepository.save(DesignEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Beta Template")
                .design("{}")
                .isCustom(true)
                .isActive(true)
                .isDeleted(false)
                .entities("invoice")
                .build());

        // All
        mockMvc.perform(get("/api/v1/designs")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total").value(2));

        // Filter
        mockMvc.perform(get("/api/v1/designs?filter=Alpha")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Alpha Template"));
    }

    @Test
    @DisplayName("GET /api/v1/designs/{id} - Should return design details")
    void getDesignById_shouldReturnDetails() throws Exception {
        DesignEntity saved = designRepository.save(DesignEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Minimalist Monolith")
                .design("{\"header\":\"<h1>Mono</h1>\"}")
                .isCustom(true)
                .isActive(true)
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/designs/" + saved.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Minimalist Monolith"));
    }

    @Test
    @DisplayName("PUT /api/v1/designs/{id} - Should update design")
    void updateDesign_shouldSucceed() throws Exception {
        DesignEntity saved = designRepository.save(DesignEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Old Design Name")
                .design("{}")
                .isCustom(true)
                .isActive(true)
                .isDeleted(false)
                .build());

        DesignDto updateDto = DesignDto.builder()
                .name("Updated Design Name")
                .isActive(true)
                .design(PartsDto.builder().header("<h2>Updated Header</h2>").build())
                .build();

        mockMvc.perform(put("/api/v1/designs/" + saved.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Design Name"))
                .andExpect(jsonPath("$.data.design.header").value("<h2>Updated Header</h2>"));
    }

    @Test
    @DisplayName("DELETE /api/v1/designs/{id} - Should soft delete design")
    void deleteDesign_shouldSoftDelete() throws Exception {
        DesignEntity saved = designRepository.save(DesignEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("To Delete")
                .design("{}")
                .isCustom(true)
                .isActive(true)
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/designs/" + saved.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/designs?status=deleted")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(saved.getId().toString()));
    }

    @Test
    @DisplayName("POST /api/v1/designs/bulk - Should archive, restore, and delete designs")
    void handleBulkAction_archiveRestoreDelete() throws Exception {
        DesignEntity d1 = designRepository.save(DesignEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Bulk Design 1")
                .design("{}")
                .isDeleted(false)
                .build());

        DesignEntity d2 = designRepository.save(DesignEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Bulk Design 2")
                .design("{}")
                .isDeleted(false)
                .build());

        // Archive
        DesignBulkActionRequest archiveReq = DesignBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(d1.getId(), d2.getId()))
                .build();

        mockMvc.perform(post("/api/v1/designs/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2));

        mockMvc.perform(get("/api/v1/designs?status=archived")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Restore
        DesignBulkActionRequest restoreReq = DesignBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(d1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/designs/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        mockMvc.perform(get("/api/v1/designs?status=active")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(d1.getId().toString()));
    }

    @Test
    @DisplayName("POST /api/v1/designs/set/default - Should set default design")
    void setDefaultDesign_shouldSucceed() throws Exception {
        SetDefaultDesignRequest request = SetDefaultDesignRequest.builder()
                .designId(UUID.randomUUID())
                .entity("invoice")
                .build();

        mockMvc.perform(post("/api/v1/designs/set/default")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/live_design - Should generate binary PDF preview stream")
    void renderLiveDesign_shouldReturnPdfByteArray() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "Dynamic Live Design",
                "header", "<h1>Invoice Live Header</h1>",
                "body", "<p>Thank you for your business.</p>"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/live_design")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 50);
        // Valid PDF starts with %PDF-
        String header = new String(pdfBytes, 0, 5);
        assertTrue(header.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Second company cannot view or modify custom designs")
    void crossTenantIsolation_cannotAccessAnotherCompanyDesign() throws Exception {
        DesignEntity design = designRepository.save(DesignEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .name("Top Secret Brand Layout")
                .design("{}")
                .isCustom(true)
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/designs")
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/v1/designs/" + design.getId())
                        .header("X-Api-Token", secondJwtToken))
                .andExpect(status().isNotFound());
    }
}
