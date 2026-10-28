package com.hashcode.invoicely.repository.relational;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.entity.relational.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IamRepositoryIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyUserRepository companyUserRepository;

    @Autowired
    private ApiTokenRepository apiTokenRepository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should persist and query full IAM entity lifecycle across relational tables")
    void shouldPersistAndQueryIamEntities() {
        // 1. Create Account
        AccountEntity account = AccountEntity.builder()
                .plan("enterprise")
                .build();
        account = accountRepository.save(account);
        assertThat(account.getId()).isNotNull();

        // 2. Create Company
        CompanyEntity company = CompanyEntity.builder()
                .accountId(account.getId())
                .companyName("Acme Global Inc.")
                .subdomain("acme-global")
                .currencyId("USD")
                .countryId("US")
                .vatNumber("VAT-US-9912")
                .build();
        company = companyRepository.save(company);
        assertThat(company.getId()).isNotNull();

        // Update default company in account
        account.setDefaultCompanyId(company.getId());
        accountRepository.save(account);

        // 3. Create User
        UserEntity user = UserEntity.builder()
                .email("admin@acme.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("$2a$12$hashedPasswordExample")
                .phone("+15551234567")
                .isRegistered(true)
                .build();
        user = userRepository.save(user);
        assertThat(user.getId()).isNotNull();

        // 4. Create CompanyUser link
        CompanyUserEntity companyUser = CompanyUserEntity.builder()
                .companyId(company.getId())
                .userId(user.getId())
                .accountId(account.getId())
                .isOwner(true)
                .isAdmin(true)
                .permissions(127L)
                .build();
        companyUser = companyUserRepository.save(companyUser);
        assertThat(companyUser.getId()).isNotNull();

        // 5. Create ApiToken
        ApiTokenEntity apiToken = ApiTokenEntity.builder()
                .companyId(company.getId())
                .userId(user.getId())
                .name("CI/CD Integration Key")
                .tokenHash("hash_abcdef123456789")
                .isSystem(false)
                .build();
        apiToken = apiTokenRepository.save(apiToken);
        assertThat(apiToken.getId()).isNotNull();

        // 6. Verify Queries
        Optional<UserEntity> queriedUser = userRepository.findByEmailIgnoreCase("ADMIN@ACME.COM");
        assertThat(queriedUser).isPresent();
        assertThat(queriedUser.get().getFirstName()).isEqualTo("John");

        Optional<CompanyEntity> queriedCompany = companyRepository.findBySubdomainAndIsDeletedFalse("acme-global");
        assertThat(queriedCompany).isPresent();
        assertThat(queriedCompany.get().getCompanyName()).isEqualTo("Acme Global Inc.");

        Optional<CompanyUserEntity> queriedLink = companyUserRepository.findByCompanyIdAndUserId(company.getId(), user.getId());
        assertThat(queriedLink).isPresent();
        assertThat(queriedLink.get().isOwner()).isTrue();

        Optional<ApiTokenEntity> queriedToken = apiTokenRepository.findByTokenHashAndIsDeletedFalse("hash_abcdef123456789");
        assertThat(queriedToken).isPresent();
        assertThat(queriedToken.get().getName()).isEqualTo("CI/CD Integration Key");
    }

    @Test
    @DisplayName("Should correctly manage TenantContext ThreadLocal scope")
    void shouldManageTenantContext() {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        TenantContext.setCompanyId(companyId);
        TenantContext.setUserId(userId);
        TenantContext.setAccountId(accountId);

        assertThat(TenantContext.getCompanyId()).isEqualTo(companyId);
        assertThat(TenantContext.getUserId()).isEqualTo(userId);
        assertThat(TenantContext.getAccountId()).isEqualTo(accountId);

        TenantContext.clear();

        assertThat(TenantContext.getCompanyId()).isNull();
        assertThat(TenantContext.getUserId()).isNull();
        assertThat(TenantContext.getAccountId()).isNull();
    }
}
