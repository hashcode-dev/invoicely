-- ==============================================================================
-- Invoicely Platform - Migration V3: Dynamic Navigation & Table Column Governance
-- ==============================================================================
-- Enables software admins and tenant owners to configure:
-- 1. Side navigation visibility and ordering per role (Customer/Admin vs Employee)
-- 2. Centralized table column defaults, locked mandatory columns, and employee restrictions
-- ==============================================================================

-- 1. Tenant Navigation Configuration
CREATE TABLE IF NOT EXISTS invoicely.tenant_navigation_configs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    role VARCHAR(32) DEFAULT 'DEFAULT' NOT NULL CHECK (role IN ('DEFAULT', 'OWNER', 'ADMIN', 'EMPLOYEE')),
    item_key VARCHAR(64) NOT NULL, -- e.g., 'dashboard', 'clients', 'invoices', 'reports', 'settings'
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    display_order INT DEFAULT 0 NOT NULL,
    custom_label VARCHAR(64),
    is_locked BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_role_nav_item UNIQUE(company_id, role, item_key)
);

CREATE INDEX IF NOT EXISTS idx_nav_company_role ON invoicely.tenant_navigation_configs(company_id, role, display_order ASC);

-- 2. Table Column Configuration & Governance
CREATE TABLE IF NOT EXISTS invoicely.table_column_configs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    table_name VARCHAR(64) NOT NULL, -- e.g., 'invoice', 'client', 'payment', 'expense', 'quote'
    default_columns TEXT DEFAULT '[]' NOT NULL, -- JSON array of strings
    locked_columns TEXT DEFAULT '[]' NOT NULL,  -- JSON array of mandatory strings
    restricted_for_employees TEXT DEFAULT '[]' NOT NULL, -- JSON array of columns hidden from employees
    allow_employee_customization BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_table_columns UNIQUE(company_id, table_name)
);

CREATE INDEX IF NOT EXISTS idx_table_column_company ON invoicely.table_column_configs(company_id, table_name);
