package com.hashcode.invoicely.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.auth.EmployeeLoginRequest;
import com.hashcode.invoicely.dto.employee.CreateEmployeeRequest;
import com.hashcode.invoicely.entity.relational.AccountEntity;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.UserEntity;
import com.hashcode.invoicely.repository.relational.AccountRepository;
import com.hashcode.invoicely.repository.relational.CompanyRepository;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import com.hashcode.invoicely.repository.relational.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerIntegrationTest {

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
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity testOwner;
    private UserEntity existingEmployee;
    private CompanyEntity testCompany;
    private AccountEntity testAccount;
    private String ownerJwtToken;
    private String employeeJwtToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(AccountEntity.builder().plan("enterprise").build());
        testCompany = companyRepository.save(CompanyEntity.builder()
                .accountId(testAccount.getId())
                .companyName("Enterprise Org")
                .subdomain("org-test")
                .currencyId("USD")
                .countryId("US")
                .build());

        testAccount.setDefaultCompanyId(testCompany.getId());
        accountRepository.save(testAccount);

        testOwner = userRepository.save(UserEntity.builder()
                .email("org-owner@invoicely.com")
                .firstName("Org")
                .lastName("Owner")
                .organizationId(testAccount.getId())
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(testOwner.getId())
                .accountId(testAccount.getId())
                .isOwner(true)
                .isAdmin(true)
                .permissions(-1L)
                .build());

        existingEmployee = userRepository.save(UserEntity.builder()
                .email("staff@invoicely.com")
                .firstName("Staff")
                .lastName("Member")
                .organizationId(testAccount.getId())
                .build());

        companyUserRepository.save(CompanyUserEntity.builder()
                .companyId(testCompany.getId())
                .userId(existingEmployee.getId())
                .accountId(testAccount.getId())
                .isOwner(false)
                .isAdmin(false)
                .permissions(15L)
                .build());

        ownerJwtToken = jwtTokenProvider.generateToken(testOwner.getId(), testOwner.getEmail(), testCompany.getId(), testAccount.getId());
        employeeJwtToken = jwtTokenProvider.generateToken(existingEmployee.getId(), existingEmployee.getEmail(), testCompany.getId(), testAccount.getId());
    }

    @Test
    @DisplayName("GET /api/v1/employees should return only employee members for tenant")
    void shouldReturnEmployeeList() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .header("X-Api-Token", ownerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email", is("staff@invoicely.com")))
                .andExpect(jsonPath("$.data[0].is_employee", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/employees/create should return blank employee template with organization id")
    void shouldReturnBlankEmployeeTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/employees/create")
                        .header("X-Api-Token", ownerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organization_id", is(testAccount.getId().toString())))
                .andExpect(jsonPath("$.data.is_employee", is(true)))
                .andExpect(jsonPath("$.data.company_user").exists())
                .andExpect(jsonPath("$.data.company_user.is_admin", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/employees should create new employee and allow immediate employee login")
    void shouldCreateEmployeeAndAllowEmployeeLogin() throws Exception {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .email("new-employee@invoicely.com")
                .firstName("Bob")
                .lastName("Smith")
                .phone("1234567890")
                .password("secret123")
                .isAdmin(false)
                .permissions(31L)
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("X-Api-Token", ownerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", is("new-employee@invoicely.com")))
                .andExpect(jsonPath("$.data.first_name", is("Bob")))
                .andExpect(jsonPath("$.data.organization_id", is(testAccount.getId().toString())))
                .andExpect(jsonPath("$.data.is_employee", is(true)))
                .andExpect(jsonPath("$.data.role", is("employee")));

        // Verify newly created employee can authenticate via Employee Login API
        EmployeeLoginRequest loginReq = EmployeeLoginRequest.builder()
                .organizationId(testAccount.getId().toString())
                .employeeId("new-employee@invoicely.com")
                .password("secret123")
                .build();

        mockMvc.perform(post("/api/v1/auth/employee_login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].user.email", is("new-employee@invoicely.com")))
                .andExpect(jsonPath("$.data[0].is_employee", is(true)))
                .andExpect(jsonPath("$.data[0].token.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/employees should fail when caller is a non-admin employee")
    void shouldRejectEmployeeCreationFromNonAdminEmployee() throws Exception {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .email("unauthorized@invoicely.com")
                .firstName("Unauthorized")
                .lastName("Create")
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("X-Api-Token", employeeJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/employees should validate required email")
    void shouldValidateInvalidEmail() throws Exception {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .email("not-an-email")
                .firstName("Invalid")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("X-Api-Token", ownerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.email", is("Email must be valid")));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} should return single employee details")
    void shouldGetEmployeeById() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + existingEmployee.getId())
                        .header("X-Api-Token", ownerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(existingEmployee.getId().toString())))
                .andExpect(jsonPath("$.data.email", is("staff@invoicely.com")));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} should update employee")
    void shouldUpdateEmployee() throws Exception {
        CreateEmployeeRequest updateReq = CreateEmployeeRequest.builder()
                .email("staff@invoicely.com")
                .firstName("StaffUpdated")
                .lastName("MemberUpdated")
                .isAdmin(true)
                .permissions(63L)
                .build();

        mockMvc.perform(put("/api/v1/employees/" + existingEmployee.getId())
                        .header("X-Api-Token", ownerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.first_name", is("StaffUpdated")))
                .andExpect(jsonPath("$.data.last_name", is("MemberUpdated")))
                .andExpect(jsonPath("$.data.company_user.is_admin", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/v1/employees/{id} should remove employee membership")
    void shouldDeleteEmployee() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/" + existingEmployee.getId())
                        .header("X-Api-Token", ownerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employees")
                        .header("X-Api-Token", ownerJwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
