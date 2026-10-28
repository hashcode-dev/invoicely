package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.vendor.VendorBulkActionRequest;
import com.hashcode.invoicely.dto.vendor.VendorContactDto;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import com.hashcode.invoicely.entity.relational.*;
import com.hashcode.invoicely.repository.relational.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VendorControllerIntegrationTest {

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
    private JwtTokenProvider jwtTokenProvider;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.hashcode.invoicely.service.SequenceGeneratorService sequenceGeneratorService;

    private final java.util.concurrent.atomic.AtomicInteger seqCounter = new java.util.concurrent.atomic.AtomicInteger(200);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private String jwtToken;

    private CompanyEntity secondCompany;
    private String secondJwtToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(AccountEntity.builder().plan("pro").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Acme Corp")
                .subdomain("acme-vendor")
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Beta LLC")
                .subdomain("beta-vendor")
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("procurement@invoicely.com")
                .firstName("Jane")
                .lastName("Buyer")
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testUser.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());

        org.mockito.Mockito.when(sequenceGeneratorService.generateSequence(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> String.format("%04d", seqCounter.incrementAndGet()));
    }

    @Test
    @DisplayName("GET /api/v1/vendors/create should return default blank vendor with sequence number")
    void shouldReturnBlankVendor() throws Exception {
        mockMvc.perform(get("/api/v1/vendors/create")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.currency_id", is("USD")))
                .andExpect(jsonPath("$.data.country_id", is("US")))
                .andExpect(jsonPath("$.data.contacts", hasSize(1)))
                .andExpect(jsonPath("$.data.contacts[0].is_primary", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/vendors should create a new vendor with contacts")
    void shouldCreateVendorWithContacts() throws Exception {
        VendorContactDto contact = VendorContactDto.builder()
                .firstName("Robert")
                .lastName("Supplier")
                .email("robert@supplier.com")
                .phone("+15554443322")
                .isPrimary(true)
                .sendEmail(true)
                .build();

        VendorDto request = VendorDto.builder()
                .name("Global Supplies Ltd")
                .website("https://globalsupplies.example.com")
                .phone("+15551112233")
                .address1("789 Industrial Blvd")
                .city("Chicago")
                .state("IL")
                .postalCode("60601")
                .countryId("US")
                .vatNumber("US11223344")
                .contacts(Collections.singletonList(contact))
                .build();

        mockMvc.perform(post("/api/v1/vendors")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is("Global Supplies Ltd")))
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.contacts", hasSize(1)))
                .andExpect(jsonPath("$.data.contacts[0].email", is("robert@supplier.com")));
    }

    @Test
    @DisplayName("GET /api/v1/vendors should return paginated list and filter by search term")
    void shouldReturnPaginatedVendorsWithFilter() throws Exception {
        vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("V001")
                .name("Office Depot Direct")
                .phone("+1800111222")
                .currencyId("USD")
                .build());

        vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("V002")
                .name("Amazon Business Services")
                .phone("+1800333444")
                .currencyId("USD")
                .build());

        // Without filter
        mockMvc.perform(get("/api/v1/vendors")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total", is(2)));

        // With filter matching "Amazon"
        mockMvc.perform(get("/api/v1/vendors?filter=Amazon")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Amazon Business Services")));

        // Multi-tenant check: secondCompany should see 0 vendors
        mockMvc.perform(get("/api/v1/vendors")
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.pagination.total", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/vendors/{id} should return vendor details")
    void shouldGetVendorById() throws Exception {
        VendorEntity vendor = VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("V010")
                .name("Fast Courier LLC")
                .phone("+18882223333")
                .currencyId("USD")
                .build();
        vendor.addContact(VendorContactEntity.builder()
                .firstName("Speedy")
                .lastName("Delivery")
                .email("dispatch@fastcourier.com")
                .isPrimary(true)
                .sendEmail(true)
                .build());
        VendorEntity saved = vendorRepository.save(vendor);

        mockMvc.perform(get("/api/v1/vendors/" + saved.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(saved.getId().toString())))
                .andExpect(jsonPath("$.data.name", is("Fast Courier LLC")))
                .andExpect(jsonPath("$.data.contacts", hasSize(1)))
                .andExpect(jsonPath("$.data.contacts[0].email", is("dispatch@fastcourier.com")));

        // Accessing from other company should yield 404
        mockMvc.perform(get("/api/v1/vendors/" + saved.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/vendors/{id} should update vendor properties and contacts")
    void shouldUpdateVendor() throws Exception {
        VendorEntity vendor = vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("V020")
                .name("Legacy Logistics")
                .currencyId("USD")
                .build());

        VendorContactDto updatedContact = VendorContactDto.builder()
                .firstName("Modern")
                .lastName("Dispatch")
                .email("modern@logistics.com")
                .isPrimary(true)
                .sendEmail(true)
                .build();

        VendorDto updateRequest = VendorDto.builder()
                .name("Modern Logistics Corp")
                .phone("+1999111222")
                .city("Atlanta")
                .contacts(Collections.singletonList(updatedContact))
                .build();

        mockMvc.perform(put("/api/v1/vendors/" + vendor.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Modern Logistics Corp")))
                .andExpect(jsonPath("$.data.city", is("Atlanta")))
                .andExpect(jsonPath("$.data.contacts", hasSize(1)))
                .andExpect(jsonPath("$.data.contacts[0].email", is("modern@logistics.com")));
    }

    @Test
    @DisplayName("POST /api/v1/vendors/bulk should handle archive, restore, and delete actions")
    void shouldHandleBulkActions() throws Exception {
        VendorEntity v1 = vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("VB01")
                .name("Vendor Alpha")
                .currencyId("USD")
                .build());

        VendorEntity v2 = vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("VB02")
                .name("Vendor Beta")
                .currencyId("USD")
                .build());

        List<UUID> ids = Arrays.asList(v1.getId(), v2.getId());

        // 1. Archive
        VendorBulkActionRequest archiveReq = VendorBulkActionRequest.builder()
                .action("archive")
                .ids(ids)
                .build();

        mockMvc.perform(post("/api/v1/vendors/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].archived_at", notNullValue()));

        // Check active list is now empty
        mockMvc.perform(get("/api/v1/vendors?status=active")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 2. Restore
        VendorBulkActionRequest restoreReq = VendorBulkActionRequest.builder()
                .action("restore")
                .ids(ids)
                .build();

        mockMvc.perform(post("/api/v1/vendors/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].archived_at", nullValue()));

        // 3. Delete
        VendorBulkActionRequest deleteReq = VendorBulkActionRequest.builder()
                .action("delete")
                .ids(ids)
                .build();

        mockMvc.perform(post("/api/v1/vendors/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].is_deleted", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/vendors/{id}/purge should permanently remove vendor")
    void shouldPurgeVendor() throws Exception {
        VendorEntity vendor = vendorRepository.save(VendorEntity.builder()
                .companyId(testCompany.getId())
                .vendorNumber("VP01")
                .name("Purge Vendor")
                .isDeleted(true)
                .archivedAt(Instant.now())
                .currencyId("USD")
                .build());

        mockMvc.perform(post("/api/v1/vendors/" + vendor.getId() + "/purge")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(vendor.getId().toString())));

        org.junit.jupiter.api.Assertions.assertFalse(vendorRepository.findById(vendor.getId()).isPresent());
    }
}
