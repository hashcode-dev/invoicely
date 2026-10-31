package com.hashcode.invoicely.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    public static final UUID DEMO_ORGANIZATION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final UUID DEMO_COMPANY_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
    public static final UUID DEMO_EMPLOYEE_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");

    public static final String DEMO_ADMIN_EMAIL = "admin@invoicely.com";
    public static final String DEMO_EMPLOYEE_EMAIL = "employee@invoicely.com";
    public static final String DEMO_PASSWORD = "password";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM invoicely.accounts WHERE id = ?", Integer.class, DEMO_ORGANIZATION_ID);
            if (count != null && count > 0) {
                return;
            }

            jdbcTemplate.update(
                    "INSERT INTO invoicely.accounts (id, plan, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    DEMO_ORGANIZATION_ID, "pro");

            jdbcTemplate.update(
                    "INSERT INTO invoicely.companies (id, account_id, company_name, currency_id, country_id, is_deleted, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    DEMO_COMPANY_ID, DEMO_ORGANIZATION_ID, "Invoicely Demo Company", "USD", "US");

            jdbcTemplate.update(
                    "UPDATE invoicely.accounts SET default_company_id = ? WHERE id = ?",
                    DEMO_COMPANY_ID, DEMO_ORGANIZATION_ID);

            String encodedPassword = passwordEncoder.encode(DEMO_PASSWORD);

            // 1. Admin Owner
            jdbcTemplate.update(
                    "INSERT INTO invoicely.users (id, email, password_hash, first_name, last_name, is_registered, auth_provider, organization_id, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, TRUE, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    UUID.fromString("770e8400-e29b-41d4-a716-446655440001"), DEMO_ADMIN_EMAIL, encodedPassword, "Admin", "Owner", "PASSWORD", DEMO_ORGANIZATION_ID);

            jdbcTemplate.update(
                    "INSERT INTO invoicely.company_users (id, company_id, user_id, account_id, is_owner, is_admin, is_locked, permissions, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, TRUE, TRUE, FALSE, -1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    UUID.fromString("880e8400-e29b-41d4-a716-446655440001"), DEMO_COMPANY_ID, UUID.fromString("770e8400-e29b-41d4-a716-446655440001"), DEMO_ORGANIZATION_ID);

            // 2. Default Employee
            jdbcTemplate.update(
                    "INSERT INTO invoicely.users (id, email, password_hash, first_name, last_name, is_registered, auth_provider, organization_id, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, TRUE, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    DEMO_EMPLOYEE_ID, DEMO_EMPLOYEE_EMAIL, encodedPassword, "Jane", "Employee", "PASSWORD", DEMO_ORGANIZATION_ID);

            jdbcTemplate.update(
                    "INSERT INTO invoicely.company_users (id, company_id, user_id, account_id, is_owner, is_admin, is_locked, permissions, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, FALSE, FALSE, FALSE, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    UUID.fromString("880e8400-e29b-41d4-a716-446655440002"), DEMO_COMPANY_ID, DEMO_EMPLOYEE_ID, DEMO_ORGANIZATION_ID);

            log.info("Initialized default demo credentials successfully -> Org ID: {}, Employee: {}, Password: {}",
                    DEMO_ORGANIZATION_ID, DEMO_EMPLOYEE_EMAIL, DEMO_PASSWORD);
        } catch (Exception e) {
            log.debug("Demo initialization skipped or already present: {}", e.getMessage());
        }
    }
}
