package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.procurement.ProductBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ProductDto;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductControllerIntegrationTest {

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
    private ProductRepository productRepository;

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

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("InventoryCorp")
                .subdomain("inv-" + uid)
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("OtherInventory")
                .subdomain("other-inv-" + uid)
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("inventory." + uid + "@invoicely.com")
                .firstName("Inventory")
                .lastName("Manager")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());

        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    @Test
    @DisplayName("GET /api/v1/products/create should return default blank product scaffold")
    void shouldReturnBlankProductScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/products/create")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product_key", is("")))
                .andExpect(jsonPath("$.data.price", is(0.0)))
                .andExpect(jsonPath("$.data.quantity", is(1.0)));
    }

    @Test
    @DisplayName("POST /api/v1/products should create product successfully")
    void shouldCreateProduct() throws Exception {
        ProductDto dto = ProductDto.builder()
                .productKey("PROD-SERVER-001")
                .notes("High performance enterprise server")
                .cost(new BigDecimal("1200.00"))
                .price(new BigDecimal("1800.00"))
                .quantity(new BigDecimal("10.00"))
                .taxName1("VAT")
                .taxRate1(new BigDecimal("20.00"))
                .inStockQuantity(new BigDecimal("25.00"))
                .stockNotification(true)
                .stockNotificationThreshold(new BigDecimal("5.00"))
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.product_key", is("PROD-SERVER-001")))
                .andExpect(jsonPath("$.data.price", is(1800.0)))
                .andExpect(jsonPath("$.data.cost", is(1200.0)))
                .andExpect(jsonPath("$.data.in_stock_quantity", is(25.0)));
    }

    @Test
    @DisplayName("GET /api/v1/products should filter products by keyword")
    void shouldFilterProducts() throws Exception {
        productRepository.save(ProductEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .productKey("WIDGET-ALPHA")
                .notes("Standard widget")
                .price(new BigDecimal("50.0000"))
                .quantity(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/products?filter=ALPHA")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].product_key", is("WIDGET-ALPHA")));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} should update product")
    void shouldUpdateProduct() throws Exception {
        ProductEntity entity = productRepository.save(ProductEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .productKey("PROD-INIT")
                .price(new BigDecimal("100.0000"))
                .isDeleted(false)
                .build());

        ProductDto updateDto = ProductDto.builder()
                .productKey("PROD-UPDATED")
                .price(new BigDecimal("150.00"))
                .notes("Updated notes")
                .build();

        mockMvc.perform(put("/api/v1/products/" + entity.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product_key", is("PROD-UPDATED")))
                .andExpect(jsonPath("$.data.price", is(150.0)));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} should soft delete product")
    void shouldSoftDeleteProduct() throws Exception {
        ProductEntity entity = productRepository.save(ProductEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .productKey("PROD-DEL")
                .price(new BigDecimal("20.0000"))
                .isDeleted(false)
                .build());

        mockMvc.perform(delete("/api/v1/products/" + entity.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ProductEntity deleted = productRepository.findById(entity.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(deleted.isDeleted());
    }

    @Test
    @DisplayName("POST /api/v1/products/bulk should archive and restore products")
    void shouldBulkArchiveAndRestoreProducts() throws Exception {
        ProductEntity entity = productRepository.save(ProductEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .productKey("PROD-BULK")
                .isDeleted(false)
                .build());

        ProductBulkActionRequest archiveReq = ProductBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(entity.getId()))
                .build();

        mockMvc.perform(post("/api/v1/products/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].archived_at", notNullValue()));

        ProductBulkActionRequest restoreReq = ProductBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(entity.getId()))
                .build();

        mockMvc.perform(post("/api/v1/products/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].archived_at", nullValue()));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot access Tenant A product")
    void shouldEnforceTenantIsolation() throws Exception {
        ProductEntity entity = productRepository.save(ProductEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .productKey("TENANT-A-SECRET-PROD")
                .isDeleted(false)
                .build());

        mockMvc.perform(get("/api/v1/products/" + entity.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
