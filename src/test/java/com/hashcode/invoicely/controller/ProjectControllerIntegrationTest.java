package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.project.ProjectBulkActionRequest;
import com.hashcode.invoicely.dto.project.ProjectDto;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProjectControllerIntegrationTest {

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
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

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
        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> "PRJ-" + seqCounter.incrementAndGet());

        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("BuildCorp")
                .subdomain("bld-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("lead-" + uid + "@buildcorp.com")
                .passwordHash("hashedPass")
                .firstName("Bob")
                .lastName("Builder")
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
                .name("Acme Software Inc")
                .clientNumber("CLI-1001")
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
    @DisplayName("GET /api/v1/projects/create - Should return blank project scaffold")
    void getBlankProject_shouldReturnScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/projects/create")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.color").value("#004bca"))
                .andExpect(jsonPath("$.data.task_rate").value(0))
                .andExpect(jsonPath("$.data.budgeted_hours").value(0))
                .andExpect(jsonPath("$.data.is_deleted").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/projects - Should create project and generate project number")
    void createProject_shouldSucceed() throws Exception {
        ProjectDto request = ProjectDto.builder()
                .clientId(testClient.getId())
                .name("Website Redesign 2026")
                .taskRate(new BigDecimal("120.0000"))
                .budgetedHours(new BigDecimal("80.0000"))
                .dueDate(LocalDate.of(2026, 12, 31))
                .color("#00AAFF")
                .publicNotes("Milestone-based project")
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Website Redesign 2026"))
                .andExpect(jsonPath("$.data.number").value(startsWith("PRJ-")))
                .andExpect(jsonPath("$.data.client_id").value(testClient.getId().toString()))
                .andExpect(jsonPath("$.data.task_rate").value(120.0))
                .andExpect(jsonPath("$.data.budgeted_hours").value(80.0));
    }

    @Test
    @DisplayName("GET /api/v1/projects - Should list projects with pagination and client details")
    void getProjects_shouldReturnList() throws Exception {
        projectRepository.save(ProjectEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .name("Alpha Project")
                .number("PRJ-001")
                .build());

        projectRepository.save(ProjectEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .name("Beta Project")
                .number("PRJ-002")
                .build());

        mockMvc.perform(get("/api/v1/projects")
                        .header("X-Api-Token", jwtToken)
                        .param("per_page", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} - Should compute current_hours based on associated tasks")
    void getProjectById_shouldComputeCurrentHours() throws Exception {
        ProjectEntity project = projectRepository.save(ProjectEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .name("Mobile App")
                .number("PRJ-003")
                .taskRate(new BigDecimal("100.0000"))
                .budgetedHours(new BigDecimal("50.0000"))
                .build());

        // Add 2 tasks: 3600s (1h) and 5400s (1.5h) => total 9000s = 2.5 hours
        taskRepository.save(TaskEntity.builder()
                .companyId(testCompany.getId())
                .projectId(project.getId())
                .clientId(testClient.getId())
                .description("Setup flutter workspace")
                .duration(3600L)
                .build());

        taskRepository.save(TaskEntity.builder()
                .companyId(testCompany.getId())
                .projectId(project.getId())
                .clientId(testClient.getId())
                .description("Auth flow implementation")
                .duration(5400L)
                .build());

        mockMvc.perform(get("/api/v1/projects/{id}", project.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(project.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Mobile App"))
                .andExpect(jsonPath("$.data.current_hours").value(2.5));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{id} - Should update project properties")
    void updateProject_shouldSucceed() throws Exception {
        ProjectEntity project = projectRepository.save(ProjectEntity.builder()
                .companyId(testCompany.getId())
                .name("Old Project Name")
                .taskRate(new BigDecimal("50.0000"))
                .build());

        ProjectDto updateDto = ProjectDto.builder()
                .name("New Project Name")
                .taskRate(new BigDecimal("150.0000"))
                .build();

        mockMvc.perform(put("/api/v1/projects/{id}", project.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Project Name"))
                .andExpect(jsonPath("$.data.task_rate").value(150.0));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id} - Should soft delete project")
    void deleteProject_shouldSucceed() throws Exception {
        ProjectEntity project = projectRepository.save(ProjectEntity.builder()
                .companyId(testCompany.getId())
                .name("Deprecated Project")
                .build());

        mockMvc.perform(delete("/api/v1/projects/{id}", project.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/v1/projects/bulk - Should archive and restore projects")
    void bulkAction_shouldArchiveAndRestore() throws Exception {
        ProjectEntity project = projectRepository.save(ProjectEntity.builder()
                .companyId(testCompany.getId())
                .name("Bulk Target Project")
                .build());

        // Archive
        ProjectBulkActionRequest archiveReq = ProjectBulkActionRequest.builder()
                .action("archive")
                .ids(List.of(project.getId()))
                .build();

        mockMvc.perform(post("/api/v1/projects/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        // Restore
        ProjectBulkActionRequest restoreReq = ProjectBulkActionRequest.builder()
                .action("restore")
                .ids(List.of(project.getId()))
                .build();

        mockMvc.perform(post("/api/v1/projects/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));
    }

    @Test
    @DisplayName("Multi-Tenancy - Projects must remain isolated across companies")
    void multiTenantIsolation_shouldNotLeak() throws Exception {
        projectRepository.save(ProjectEntity.builder()
                .companyId(secondCompany.getId())
                .name("Tenant 2 Project")
                .build());

        mockMvc.perform(get("/api/v1/projects")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
