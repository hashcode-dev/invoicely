package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.project.TaskStatusDto;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskStatusControllerIntegrationTest {

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
    private TaskStatusRepository taskStatusRepository;

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
        testAccount = accountRepository.save(AccountEntity.builder().plan("pro").build());

        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("AgileCorp")
                .subdomain("agile-" + uid)
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("lead-" + uid + "@agilecorp.com")
                .passwordHash("hashedPass")
                .firstName("Sarah")
                .lastName("Connor")
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
                .subdomain("other-" + uid)
                .build());

        jwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), testCompany.getId(), testAccount.getId());
        secondJwtToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail(), secondCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("POST /api/v1/task_statuses - Should create task status successfully")
    void createTaskStatus_shouldSucceed() throws Exception {
        TaskStatusDto request = TaskStatusDto.builder()
                .name("In Review")
                .color("#FFA500")
                .sortOrder(2)
                .build();

        mockMvc.perform(post("/api/v1/task_statuses")
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("In Review"))
                .andExpect(jsonPath("$.data.color").value("#FFA500"))
                .andExpect(jsonPath("$.data.sort_order").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/task_statuses - Should return list of task statuses with pagination")
    void getTaskStatuses_shouldReturnList() throws Exception {
        taskStatusRepository.save(TaskStatusEntity.builder()
                .companyId(testCompany.getId())
                .name("To Do")
                .color("#808080")
                .sortOrder(1)
                .build());

        taskStatusRepository.save(TaskStatusEntity.builder()
                .companyId(testCompany.getId())
                .name("In Progress")
                .color("#0000FF")
                .sortOrder(2)
                .build());

        mockMvc.perform(get("/api/v1/task_statuses")
                        .header("X-Api-Token", jwtToken)
                        .param("per_page", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.meta.pagination.total").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/task_statuses/{id} - Should return task status by ID")
    void getTaskStatusById_shouldSucceed() throws Exception {
        TaskStatusEntity entity = taskStatusRepository.save(TaskStatusEntity.builder()
                .companyId(testCompany.getId())
                .name("Done")
                .color("#008000")
                .sortOrder(3)
                .build());

        mockMvc.perform(get("/api/v1/task_statuses/{id}", entity.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(entity.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Done"));
    }

    @Test
    @DisplayName("PUT /api/v1/task_statuses/{id} - Should update task status")
    void updateTaskStatus_shouldSucceed() throws Exception {
        TaskStatusEntity entity = taskStatusRepository.save(TaskStatusEntity.builder()
                .companyId(testCompany.getId())
                .name("Backlog")
                .color("#000000")
                .sortOrder(0)
                .build());

        TaskStatusDto updateDto = TaskStatusDto.builder()
                .name("Icebox")
                .color("#CCCCCC")
                .sortOrder(0)
                .build();

        mockMvc.perform(put("/api/v1/task_statuses/{id}", entity.getId())
                        .header("X-Api-Token", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Icebox"))
                .andExpect(jsonPath("$.data.color").value("#CCCCCC"));
    }

    @Test
    @DisplayName("DELETE /api/v1/task_statuses/{id} - Should soft delete task status")
    void deleteTaskStatus_shouldSucceed() throws Exception {
        TaskStatusEntity entity = taskStatusRepository.save(TaskStatusEntity.builder()
                .companyId(testCompany.getId())
                .name("Obsolete")
                .build());

        mockMvc.perform(delete("/api/v1/task_statuses/{id}", entity.getId())
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk());

        // Should no longer appear in default active list
        mockMvc.perform(get("/api/v1/task_statuses")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("Multi-Tenancy - Task statuses must remain isolated across companies")
    void multiTenantIsolation_shouldNotLeak() throws Exception {
        taskStatusRepository.save(TaskStatusEntity.builder()
                .companyId(secondCompany.getId())
                .name("Secret Status")
                .build());

        mockMvc.perform(get("/api/v1/task_statuses")
                        .header("X-Api-Token", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
