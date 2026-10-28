package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.project.TaskBulkActionRequest;
import com.hashcode.invoicely.dto.project.TaskDto;
import com.hashcode.invoicely.dto.project.TaskSortRequest;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskControllerIntegrationTest {

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
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SequenceGeneratorService sequenceGeneratorService;

    private final AtomicInteger seqCounter = new AtomicInteger(100);

    private UserEntity testUser;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private ClientEntity testClient;
    private ProjectEntity testProject;
    private TaskStatusEntity testStatus;
    private String jwtToken;

    private CompanyEntity secondCompany;
    private String secondJwtToken;

    @BeforeEach
    void setUp() {
        when(sequenceGeneratorService.generateSequence(anyString()))
                .thenAnswer(inv -> "TSK-" + seqCounter.incrementAndGet());

        String uid = UUID.randomUUID().toString().substring(0, 8);
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("DevOpsCorp")
                .subdomain("dvo-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("lead-" + uid + "@devopscorp.com")
                .passwordHash("hashedPass")
                .firstName("Alice")
                .lastName("Engineer")
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
                .name("Global Tech Partners")
                .clientNumber("CLI-5001")
                .balance(BigDecimal.ZERO)
                .paidToDate(BigDecimal.ZERO)
                .build());

        testProject = projectRepository.save(ProjectEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .name("Kubernetes Migration")
                .taskRate(new BigDecimal("150.0000"))
                .build());

        testStatus = taskStatusRepository.save(TaskStatusEntity.builder()
                .companyId(testCompany.getId())
                .name("In Progress")
                .sortOrder(1)
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
    @DisplayName("GET /api/v1/tasks/create - Should return blank task scaffold")
    void getBlankTask_shouldReturnScaffold() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/create")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duration").value(0))
                .andExpect(jsonPath("$.data.is_running").value(false))
                .andExpect(jsonPath("$.data.time_log").value("[]"))
                .andExpect(jsonPath("$.data.rate").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/tasks - Should create task and inherit project client & rate")
    void createTask_shouldInheritProjectDetails() throws Exception {
        TaskDto request = TaskDto.builder()
                .projectId(testProject.getId())
                .statusId(testStatus.getId())
                .description("Configure Helm charts")
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.description").value("Configure Helm charts"))
                .andExpect(jsonPath("$.data.project_id").value(testProject.getId().toString()))
                .andExpect(jsonPath("$.data.client_id").value(testClient.getId().toString()))
                .andExpect(jsonPath("$.data.rate").value(150.0))
                .andExpect(jsonPath("$.data.is_running").value(false));
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{id}?start=true and ?stop=true - Should control real-time timer and compute duration")
    void updateTask_timerLifecycle() throws Exception {
        TaskEntity task = taskRepository.save(TaskEntity.builder()
                .companyId(testCompany.getId())
                .projectId(testProject.getId())
                .clientId(testClient.getId())
                .description("Investigate memory leak")
                .duration(0L)
                .isRunning(false)
                .timeLog("[]")
                .build());

        // 1. Start timer
        mockMvc.perform(put("/api/v1/tasks/{id}?start=true", task.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_running").value(true))
                .andExpect(jsonPath("$.data.time_log").value(not("[]")));

        // 2. Stop timer
        mockMvc.perform(put("/api/v1/tasks/{id}?stop=true", task.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_running").value(false));

        TaskEntity refreshed = taskRepository.findById(task.getId()).orElseThrow();
        assertFalse(refreshed.getIsRunning());
        assertNotNull(refreshed.getTimeLog());
    }

    @Test
    @DisplayName("POST /api/v1/tasks/sort - Should reorder tasks in Kanban board")
    void sortTasks_kanbanReorder() throws Exception {
        TaskStatusEntity newColumn = taskStatusRepository.save(TaskStatusEntity.builder()
                .companyId(testCompany.getId())
                .name("QA Verify")
                .sortOrder(2)
                .build());

        TaskEntity t1 = taskRepository.save(TaskEntity.builder()
                .companyId(testCompany.getId())
                .description("Write unit tests")
                .build());

        TaskEntity t2 = taskRepository.save(TaskEntity.builder()
                .companyId(testCompany.getId())
                .description("Deploy staging")
                .build());

        TaskSortRequest sortRequest = TaskSortRequest.builder()
                .statusIds(List.of(newColumn.getId()))
                .taskIds(Map.of(
                        newColumn.getId().toString(), List.of(t2.getId(), t1.getId())
                ))
                .build();

        mockMvc.perform(post("/api/v1/tasks/sort")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sortRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        TaskEntity refreshedT2 = taskRepository.findById(t2.getId()).orElseThrow();
        assertEquals(newColumn.getId(), refreshedT2.getStatusId());
        assertEquals(0, refreshedT2.getStatusOrder());

        TaskEntity refreshedT1 = taskRepository.findById(t1.getId()).orElseThrow();
        assertEquals(newColumn.getId(), refreshedT1.getStatusId());
        assertEquals(1, refreshedT1.getStatusOrder());
    }

    @Test
    @DisplayName("POST /api/v1/tasks/bulk - Should convert uninvoiced tasks to draft invoice and credit client balance")
    void bulkAction_invoiceTasks() throws Exception {
        TaskEntity t1 = taskRepository.save(TaskEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("TSK-1")
                .description("Backend architecture")
                .rate(new BigDecimal("100.0000"))
                .duration(7200L) // 2 hours
                .build());

        TaskEntity t2 = taskRepository.save(TaskEntity.builder()
                .companyId(testCompany.getId())
                .clientId(testClient.getId())
                .number("TSK-2")
                .description("API implementation")
                .rate(new BigDecimal("100.0000"))
                .duration(3600L) // 1 hour
                .build());

        TaskBulkActionRequest invoiceRequest = TaskBulkActionRequest.builder()
                .action("invoice_tasks")
                .ids(List.of(t1.getId(), t2.getId()))
                .build();

        mockMvc.perform(post("/api/v1/tasks/bulk")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2))
                .andExpect(jsonPath("$.invoice_id").isNotEmpty())
                .andExpect(jsonPath("$.invoice_number").isNotEmpty());

        // Verify task links and invoice
        TaskEntity refreshedT1 = taskRepository.findById(t1.getId()).orElseThrow();
        assertNotNull(refreshedT1.getInvoiceId());

        InvoiceEntity invoice = invoiceRepository.findById(refreshedT1.getInvoiceId()).orElseThrow();
        assertEquals(testClient.getId(), invoice.getClientId());
        assertEquals(2, invoice.getLineItems().size());
        // 2h * 100 + 1h * 100 = $300
        assertEquals(new BigDecimal("300.0000"), invoice.getAmount());

        ClientEntity refreshedClient = clientRepository.findById(testClient.getId()).orElseThrow();
        assertEquals(new BigDecimal("300.0000"), refreshedClient.getBalance());
    }

    @Test
    @DisplayName("GET /api/v1/calendar_connection/events - Should return stub calendar events")
    void calendarConnection_shouldReturnEvents() throws Exception {
        mockMvc.perform(get("/api/v1/calendar_connection/events")
                        .header("X-Api-Token", jwtToken)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events").isArray());

        mockMvc.perform(post("/api/v1/one_time_token")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hash").isNotEmpty());
    }

    @Test
    @DisplayName("Multi-Tenancy - Tasks must remain isolated across companies")
    void multiTenantIsolation_shouldNotLeak() throws Exception {
        taskRepository.save(TaskEntity.builder()
                .companyId(secondCompany.getId())
                .description("Secret Second Tenant Task")
                .build());

        mockMvc.perform(get("/api/v1/tasks")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
