package com.hashcode.invoicely.context;

import java.util.UUID;

/**
 * ThreadLocal context holder for multi-tenant isolation.
 * Automatically populated per-request by TenantContextFilter from headers/JWT tokens.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_COMPANY_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_ACCOUNT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static UUID getCompanyId() {
        return CURRENT_COMPANY_ID.get();
    }

    public static void setCompanyId(UUID companyId) {
        CURRENT_COMPANY_ID.set(companyId);
    }

    public static UUID getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void setUserId(UUID userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static UUID getAccountId() {
        return CURRENT_ACCOUNT_ID.get();
    }

    public static void setAccountId(UUID accountId) {
        CURRENT_ACCOUNT_ID.set(accountId);
    }

    public static void clear() {
        CURRENT_COMPANY_ID.remove();
        CURRENT_USER_ID.remove();
        CURRENT_ACCOUNT_ID.remove();
    }
}
