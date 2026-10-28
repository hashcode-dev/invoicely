package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.client.ClientBulkActionRequest;
import com.hashcode.invoicely.dto.client.ClientContactDto;
import com.hashcode.invoicely.dto.client.ClientDto;
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

import java.math.BigDecimal;
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
class ClientControllerIntegrationTest {

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
    private JwtTokenProvider jwtTokenProvider;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.hashcode.invoicely.service.SequenceGeneratorService sequenceGeneratorService;

    private final java.util.concurrent.atomic.AtomicInteger seqCounter = new java.util.concurrent.atomic.AtomicInteger(100);

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
                .subdomain("acme")
                .currencyId("USD")
                .countryId("US")
                .build());

        secondCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Beta LLC")
                .subdomain("beta")
                .currencyId("EUR")
                .countryId("DE")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("john.client@invoicely.com")
                .firstName("John")
                .lastName("Doe")
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
    @DisplayName("GET /api/v1/clients/create should return default blank client with sequence number")
    void shouldReturnBlankClient() throws Exception {
        mockMvc.perform(get("/api/v1/clients/create")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.currency_id", is("USD")))
                .andExpect(jsonPath("$.data.country_id", is("US")))
                .andExpect(jsonPath("$.data.balance", is(0)))
                .andExpect(jsonPath("$.data.contacts", hasSize(1)))
                .andExpect(jsonPath("$.data.contacts[0].is_primary", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/clients should create a new client with contacts")
    void shouldCreateClientWithContacts() throws Exception {
        ClientContactDto contact1 = ClientContactDto.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@globex.com")
                .phone("+15551234567")
                .isPrimary(true)
                .sendEmail(true)
                .build();

        ClientContactDto contact2 = ClientContactDto.builder()
                .firstName("Bob")
                .lastName("Jones")
                .email("bob@globex.com")
                .phone("+15557654321")
                .isPrimary(false)
                .sendEmail(false)
                .build();

        ClientDto request = ClientDto.builder()
                .name("Globex Corporation")
                .website("https://globex.example.com")
                .phone("+15559876543")
                .address1("123 Springfield Way")
                .city("Springfield")
                .state("OR")
                .postalCode("97477")
                .countryId("US")
                .vatNumber("US987654321")
                .contacts(Arrays.asList(contact1, contact2))
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is("Globex Corporation")))
                .andExpect(jsonPath("$.data.number", notNullValue()))
                .andExpect(jsonPath("$.data.contacts", hasSize(2)))
                .andExpect(jsonPath("$.data.contacts[0].email", is("alice@globex.com")))
                .andExpect(jsonPath("$.data.contacts[0].is_primary", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/clients should return paginated list and filter by search term")
    void shouldReturnPaginatedClientsWithFilter() throws Exception {
        ClientEntity c1 = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0001")
                .name("Alpha Tech Inc")
                .phone("+111222333")
                .currencyId("USD")
                .build());

        ClientEntity c2 = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0002")
                .name("Omega Solutions")
                .phone("+444555666")
                .currencyId("USD")
                .build());

        // Without filter
        mockMvc.perform(get("/api/v1/clients")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total", is(2)));

        // With filter matching "Alpha"
        mockMvc.perform(get("/api/v1/clients?filter=Alpha")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Alpha Tech Inc")));

        // Multi-tenant check: secondCompany should see 0 clients
        mockMvc.perform(get("/api/v1/clients")
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.pagination.total", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/clients/{id} should return single client details")
    void shouldGetClientById() throws Exception {
        ClientEntity client = ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0005")
                .name("Stark Industries")
                .phone("+18005550199")
                .currencyId("USD")
                .build();
        client.addContact(ClientContactEntity.builder()
                .firstName("Pepper")
                .lastName("Potts")
                .email("pepper@stark.com")
                .isPrimary(true)
                .sendEmail(true)
                .build());
        ClientEntity saved = clientRepository.save(client);

        mockMvc.perform(get("/api/v1/clients/" + saved.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(saved.getId().toString())))
                .andExpect(jsonPath("$.data.name", is("Stark Industries")))
                .andExpect(jsonPath("$.data.contacts", hasSize(1)))
                .andExpect(jsonPath("$.data.contacts[0].email", is("pepper@stark.com")));

        // Accessing from unauthorized tenant should return 404
        mockMvc.perform(get("/api/v1/clients/" + saved.getId())
                        .header("X-Api-Token", secondJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/clients/{id} should update client properties and contacts")
    void shouldUpdateClient() throws Exception {
        ClientEntity client = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0010")
                .name("Wayne Enterprises")
                .currencyId("USD")
                .build());

        ClientContactDto updatedContact = ClientContactDto.builder()
                .firstName("Lucius")
                .lastName("Fox")
                .email("lucius@wayne.com")
                .isPrimary(true)
                .sendEmail(true)
                .build();

        ClientDto updateRequest = ClientDto.builder()
                .name("Wayne Enterprises International")
                .phone("+1999888777")
                .city("Gotham")
                .contacts(Collections.singletonList(updatedContact))
                .build();

        mockMvc.perform(put("/api/v1/clients/" + client.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Wayne Enterprises International")))
                .andExpect(jsonPath("$.data.city", is("Gotham")))
                .andExpect(jsonPath("$.data.contacts", hasSize(1)))
                .andExpect(jsonPath("$.data.contacts[0].email", is("lucius@wayne.com")));
    }

    @Test
    @DisplayName("POST /api/v1/clients/bulk should handle archive, restore, delete, and clone actions")
    void shouldHandleBulkActions() throws Exception {
        ClientEntity c1 = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0021")
                .name("Client One")
                .currencyId("USD")
                .build());

        ClientEntity c2 = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0022")
                .name("Client Two")
                .currencyId("USD")
                .build());

        List<UUID> ids = Arrays.asList(c1.getId(), c2.getId());

        // 1. Archive
        ClientBulkActionRequest archiveReq = ClientBulkActionRequest.builder()
                .action("archive")
                .ids(ids)
                .build();

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].archived_at", notNullValue()));

        // Check active list is now empty
        mockMvc.perform(get("/api/v1/clients?status=active")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 2. Restore
        ClientBulkActionRequest restoreReq = ClientBulkActionRequest.builder()
                .action("restore")
                .ids(ids)
                .build();

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].archived_at", nullValue()));

        // 3. Clone
        ClientBulkActionRequest cloneReq = ClientBulkActionRequest.builder()
                .action("clone")
                .ids(Collections.singletonList(c1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cloneReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Client One (Copy)")));

        // 4. Delete
        ClientBulkActionRequest deleteReq = ClientBulkActionRequest.builder()
                .action("delete")
                .ids(ids)
                .build();

        mockMvc.perform(post("/api/v1/clients/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].is_deleted", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/clients/{targetId}/{sourceId}/merge should aggregate balances and delete source")
    void shouldMergeClients() throws Exception {
        ClientEntity target = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0031")
                .name("Target Enterprise")
                .balance(new BigDecimal("150.0000"))
                .paidToDate(new BigDecimal("50.0000"))
                .currencyId("USD")
                .build());

        ClientEntity source = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0032")
                .name("Source Sub-Org")
                .balance(new BigDecimal("250.0000"))
                .paidToDate(new BigDecimal("100.0000"))
                .currencyId("USD")
                .build());

        mockMvc.perform(post("/api/v1/clients/" + target.getId() + "/" + source.getId() + "/merge")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(target.getId().toString())))
                .andExpect(jsonPath("$.data.balance", is(400.0)))
                .andExpect(jsonPath("$.data.paid_to_date", is(150.0)));

        // Verify source is soft deleted
        ClientEntity reloadedSource = clientRepository.findById(source.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(reloadedSource.isDeleted());
    }

    @Test
    @DisplayName("POST /api/v1/clients/{id}/purge should permanently remove deleted client")
    void shouldPurgeClient() throws Exception {
        ClientEntity client = clientRepository.save(ClientEntity.builder()
                .companyId(testCompany.getId())
                .clientNumber("0040")
                .name("To Be Purged")
                .isDeleted(true)
                .archivedAt(Instant.now())
                .currencyId("USD")
                .build());

        mockMvc.perform(post("/api/v1/clients/" + client.getId() + "/purge")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(client.getId().toString())));

        org.junit.jupiter.api.Assertions.assertFalse(clientRepository.findById(client.getId()).isPresent());
    }
}
