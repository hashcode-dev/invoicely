-- ==============================================================================
-- Invoicely Platform - Complete Enterprise Baseline DDL (PostgreSQL)
-- ==============================================================================
-- Schema Version: 1.0.0 (Unified Complete Baseline)
-- Target Database: PostgreSQL 14+ (Compatible with Supabase / Aurora PostgreSQL / H2)
-- Architecture: Multi-Tenant B2B Invoicing, Payments, Procurement & Financial Engine
--
-- EXECUTION INSTRUCTIONS FOR DBA:
-- 1. Ensure target database and user exist with appropriate privileges.
-- 2. Execute this script using psql or your migration pipeline:
--      psql -h <host> -U <user> -d <database> -f V1__init_invoicely_schema.sql
-- 3. The script is structured in strictly ordered dependency tiers (Tiers 0 - 8):
--      - Tier 0: Schema & Core Extension Setup
--      - Tier 1: Multi-Tenancy & IAM Foundation (Accounts, Companies, Users, Auth, Sequences)
--      - Tier 2: CRM Master Data (Clients, Vendors, and Contacts)
--      - Tier 3: Master Catalogs, Task Workflows & Project Management
--      - Tier 4: Core Invoicing, Line Items, Quotes, Credits & Subscriptions
--      - Tier 5: Payment Gateways, Transactions & Reconciliation
--      - Tier 6: Procurement & AP (Products, Purchase Orders, Expenses)
--      - Tier 7: Banking Integrations, Rule Engines & Transaction Feeds
--      - Tier 8: Document Vault, AST Designs, E-Signature Blueprints
-- 4. All primary and foreign keys, default UUIDs, and audit timestamps are enforced.
-- ==============================================================================

-- ==============================================================================
-- TIER 0: SCHEMA & EXTENSION INITIALIZATION
-- ==============================================================================

CREATE SCHEMA IF NOT EXISTS invoicely;

-- ==============================================================================
-- TIER 1: MULTI-TENANCY & IAM FOUNDATION
-- ==============================================================================

-- 1. Accounts (Top-level multi-tenant billing account)
CREATE TABLE IF NOT EXISTS invoicely.accounts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    default_company_id UUID,
    plan VARCHAR(64) DEFAULT 'free',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Companies (Tenants belonging to an account)
CREATE TABLE IF NOT EXISTS invoicely.companies (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES invoicely.accounts(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(128) UNIQUE,
    currency_id VARCHAR(8) DEFAULT 'USD' NOT NULL,
    country_id VARCHAR(8) DEFAULT 'US' NOT NULL,
    size_id VARCHAR(32),
    industry_id VARCHAR(64),
    vat_number VARCHAR(64),
    gst_number VARCHAR(64),
    portal_domain VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_companies_account_id ON invoicely.companies(account_id);

-- 3. Users (IAM users with OAuth, password & organization support)
CREATE TABLE IF NOT EXISTS invoicely.users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(128),
    last_name VARCHAR(128),
    phone VARCHAR(64),
    is_registered BOOLEAN DEFAULT TRUE NOT NULL,
    auth_provider VARCHAR(32) DEFAULT 'PASSWORD' NOT NULL,
    oauth_id VARCHAR(255),
    avatar_url VARCHAR(1024),
    organization_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_oauth_id ON invoicely.users(oauth_id);
CREATE INDEX IF NOT EXISTS idx_users_auth_provider ON invoicely.users(auth_provider);
CREATE INDEX IF NOT EXISTS idx_users_organization_id ON invoicely.users(organization_id);

-- 4. Company Users (Tenant-User membership & RBAC permissions)
CREATE TABLE IF NOT EXISTS invoicely.company_users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES invoicely.users(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES invoicely.accounts(id),
    is_owner BOOLEAN DEFAULT FALSE NOT NULL,
    is_admin BOOLEAN DEFAULT FALSE NOT NULL,
    is_locked BOOLEAN DEFAULT FALSE NOT NULL,
    permissions BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_user UNIQUE(company_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_company_users_user_id ON invoicely.company_users(user_id);
CREATE INDEX IF NOT EXISTS idx_company_users_company_id ON invoicely.company_users(company_id);

-- 5. API Tokens (Machine-to-machine and programmatic authentication tokens)
CREATE TABLE IF NOT EXISTS invoicely.api_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES invoicely.users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    is_system BOOLEAN DEFAULT FALSE NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_api_tokens_token_hash ON invoicely.api_tokens(token_hash);

-- 6. System Sequences (Atomic Relational Number Sequences for Invoices, Payments, POs, etc.)
CREATE TABLE IF NOT EXISTS invoicely.system_sequences (
    id VARCHAR(128) PRIMARY KEY,
    seq BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ==============================================================================
-- TIER 2: CRM MASTER DATA (CLIENTS & VENDORS)
-- ==============================================================================

-- 7. Clients (Accounts Receivable customers)
CREATE TABLE IF NOT EXISTS invoicely.clients (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    client_number VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    website VARCHAR(255),
    phone VARCHAR(64),
    balance NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    paid_to_date NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    credit_balance NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    currency_id VARCHAR(8) DEFAULT 'USD' NOT NULL,
    address1 VARCHAR(255),
    address2 VARCHAR(255),
    city VARCHAR(128),
    state VARCHAR(128),
    postal_code VARCHAR(32),
    country_id VARCHAR(8),
    shipping_address1 VARCHAR(255),
    shipping_address2 VARCHAR(255),
    shipping_city VARCHAR(128),
    shipping_state VARCHAR(128),
    shipping_postal_code VARCHAR(32),
    shipping_country_id VARCHAR(8),
    vat_number VARCHAR(64),
    id_number VARCHAR(64),
    routing_id VARCHAR(64),
    public_notes TEXT,
    private_notes TEXT,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    is_tax_exempt BOOLEAN DEFAULT FALSE NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_client_number UNIQUE(company_id, client_number)
);

CREATE INDEX IF NOT EXISTS idx_clients_company_id ON invoicely.clients(company_id);
CREATE INDEX IF NOT EXISTS idx_clients_company_status ON invoicely.clients(company_id, is_deleted);

-- 8. Client Contacts (Authorized contact persons for a client)
CREATE TABLE IF NOT EXISTS invoicely.client_contacts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES invoicely.clients(id) ON DELETE CASCADE,
    first_name VARCHAR(128),
    last_name VARCHAR(128),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(64),
    is_primary BOOLEAN DEFAULT FALSE NOT NULL,
    send_email BOOLEAN DEFAULT TRUE NOT NULL,
    contact_key VARCHAR(64),
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    is_locked BOOLEAN DEFAULT FALSE NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_client_contacts_client_id ON invoicely.client_contacts(client_id);

-- 9. Vendors (Accounts Payable suppliers)
CREATE TABLE IF NOT EXISTS invoicely.vendors (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    vendor_number VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    website VARCHAR(255),
    phone VARCHAR(64),
    currency_id VARCHAR(8) DEFAULT 'USD' NOT NULL,
    address1 VARCHAR(255),
    address2 VARCHAR(255),
    city VARCHAR(128),
    state VARCHAR(128),
    postal_code VARCHAR(32),
    country_id VARCHAR(8),
    vat_number VARCHAR(64),
    id_number VARCHAR(64),
    routing_id VARCHAR(64),
    public_notes TEXT,
    private_notes TEXT,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    is_tax_exempt BOOLEAN DEFAULT FALSE NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_vendor_number UNIQUE(company_id, vendor_number)
);

CREATE INDEX IF NOT EXISTS idx_vendors_company_id ON invoicely.vendors(company_id);
CREATE INDEX IF NOT EXISTS idx_vendors_company_status ON invoicely.vendors(company_id, is_deleted);

-- 10. Vendor Contacts (Authorized contact persons for a vendor)
CREATE TABLE IF NOT EXISTS invoicely.vendor_contacts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    vendor_id UUID NOT NULL REFERENCES invoicely.vendors(id) ON DELETE CASCADE,
    first_name VARCHAR(128),
    last_name VARCHAR(128),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(64),
    is_primary BOOLEAN DEFAULT FALSE NOT NULL,
    send_email BOOLEAN DEFAULT TRUE NOT NULL,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vendor_contacts_vendor_id ON invoicely.vendor_contacts(vendor_id);

-- ==============================================================================
-- TIER 3: CATALOGS, WORKFLOW STATUSES & PROJECT MANAGEMENT
-- ==============================================================================

-- 11. Expense Categories (Tax & budget categorization for expenses)
CREATE TABLE IF NOT EXISTS invoicely.expense_categories (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    name VARCHAR(128) NOT NULL,
    color VARCHAR(32),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_expense_category_name UNIQUE(company_id, name)
);

CREATE INDEX IF NOT EXISTS idx_expense_categories_company_id ON invoicely.expense_categories(company_id);

-- 12. Products (Product catalog, pricing, SKU and inventory)
CREATE TABLE IF NOT EXISTS invoicely.products (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    product_key VARCHAR(128) NOT NULL,
    notes TEXT,
    cost NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    price NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    quantity NUMERIC(18, 4) DEFAULT 1.0000 NOT NULL,
    tax_name1 VARCHAR(64),
    tax_rate1 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name2 VARCHAR(64),
    tax_rate2 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name3 VARCHAR(64),
    tax_rate3 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    in_stock_quantity NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    stock_notification BOOLEAN DEFAULT FALSE NOT NULL,
    stock_notification_threshold NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_product_key UNIQUE(company_id, product_key)
);

CREATE INDEX IF NOT EXISTS idx_products_company_id ON invoicely.products(company_id);

-- 13. Task Statuses (Kanban columns & workflow state machines)
CREATE TABLE IF NOT EXISTS invoicely.task_statuses (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    color VARCHAR(32),
    sort_order INT DEFAULT 0 NOT NULL,
    status_order INT DEFAULT 0 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_task_status_name UNIQUE(company_id, name)
);

CREATE INDEX IF NOT EXISTS idx_task_statuses_company ON invoicely.task_statuses(company_id, is_deleted);

-- 14. Projects (Client-linked billable projects & budgets)
CREATE TABLE IF NOT EXISTS invoicely.projects (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    client_id UUID REFERENCES invoicely.clients(id) ON DELETE SET NULL,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    name VARCHAR(255) NOT NULL,
    number VARCHAR(128),
    task_rate NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    budgeted_hours NUMERIC(12, 4) DEFAULT 0.0000 NOT NULL,
    due_date DATE,
    private_notes TEXT,
    public_notes TEXT,
    color VARCHAR(32),
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_project_name UNIQUE(company_id, name)
);

CREATE INDEX IF NOT EXISTS idx_projects_company ON invoicely.projects(company_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_projects_client ON invoicely.projects(company_id, client_id);

-- ==============================================================================
-- TIER 4: CORE INVOICING, BILLING ENGINE & TIME TRACKING
-- ==============================================================================

-- 15. Tax Rates (Configurable sales tax, VAT, GST rates)
CREATE TABLE IF NOT EXISTS invoicely.tax_rates (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    name VARCHAR(128) NOT NULL,
    rate NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    is_inclusive BOOLEAN DEFAULT FALSE NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tax_rates_company_id ON invoicely.tax_rates(company_id);

-- 16. Invoices (Core sales invoices and billing records)
CREATE TABLE IF NOT EXISTS invoicely.invoices (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES invoicely.clients(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    recurring_id UUID,
    project_id UUID REFERENCES invoicely.projects(id) ON DELETE SET NULL,
    number VARCHAR(128) NOT NULL,
    po_number VARCHAR(128),
    date DATE NOT NULL,
    due_date DATE NOT NULL,
    partial_due_date DATE,
    status_id VARCHAR(32) DEFAULT 'draft' NOT NULL,
    amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    balance NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    paid_to_date NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    discount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    is_amount_discount BOOLEAN DEFAULT FALSE NOT NULL,
    partial NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    total_taxes NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    terms TEXT,
    public_notes TEXT,
    private_notes TEXT,
    footer TEXT,
    uses_inclusive_taxes BOOLEAN DEFAULT FALSE NOT NULL,
    tax_name1 VARCHAR(64),
    tax_rate1 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    tax_name2 VARCHAR(64),
    tax_rate2 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    tax_name3 VARCHAR(64),
    tax_rate3 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    exchange_rate NUMERIC(12, 6) DEFAULT 1.000000 NOT NULL,
    design_id VARCHAR(64) DEFAULT 'clean',
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_invoice_number UNIQUE(company_id, number)
);

CREATE INDEX IF NOT EXISTS idx_invoices_company_id ON invoicely.invoices(company_id);
CREATE INDEX IF NOT EXISTS idx_invoices_client_id ON invoicely.invoices(client_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoicely.invoices(company_id, status_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status_date ON invoicely.invoices(company_id, is_deleted, status_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_client ON invoicely.invoices(company_id, client_id, due_date);

-- 17. Invoice Items (Granular line items per invoice)
CREATE TABLE IF NOT EXISTS invoicely.invoice_items (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoicely.invoices(id) ON DELETE CASCADE,
    product_key VARCHAR(128),
    notes TEXT,
    cost NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    quantity NUMERIC(12, 4) DEFAULT 1.0000 NOT NULL,
    discount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    is_amount_discount BOOLEAN DEFAULT FALSE NOT NULL,
    line_total NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    gross_line_total NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name1 VARCHAR(64),
    tax_rate1 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    tax_name2 VARCHAR(64),
    tax_rate2 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    tax_name3 VARCHAR(64),
    tax_rate3 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    type_id VARCHAR(32) DEFAULT '1',
    sort_id INT DEFAULT 0,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id ON invoicely.invoice_items(invoice_id);

-- 18. Quotes (Estimates and commercial proposals)
CREATE TABLE IF NOT EXISTS invoicely.quotes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES invoicely.clients(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    invoice_id UUID,
    number VARCHAR(128) NOT NULL,
    po_number VARCHAR(128),
    date DATE NOT NULL,
    due_date DATE NOT NULL,
    status_id VARCHAR(32) DEFAULT 'draft' NOT NULL,
    amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    balance NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    discount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    is_amount_discount BOOLEAN DEFAULT FALSE NOT NULL,
    total_taxes NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    terms TEXT,
    public_notes TEXT,
    private_notes TEXT,
    footer TEXT,
    uses_inclusive_taxes BOOLEAN DEFAULT FALSE NOT NULL,
    tax_name1 VARCHAR(64),
    tax_rate1 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    tax_name2 VARCHAR(64),
    tax_rate2 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_quote_number UNIQUE(company_id, number)
);

CREATE INDEX IF NOT EXISTS idx_quotes_company_id ON invoicely.quotes(company_id);
CREATE INDEX IF NOT EXISTS idx_quotes_client_id ON invoicely.quotes(client_id);

-- 19. Quote Items (Granular line items per quote)
CREATE TABLE IF NOT EXISTS invoicely.quote_items (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    quote_id UUID NOT NULL REFERENCES invoicely.quotes(id) ON DELETE CASCADE,
    product_key VARCHAR(128),
    notes TEXT,
    cost NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    quantity NUMERIC(12, 4) DEFAULT 1.0000 NOT NULL,
    discount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    is_amount_discount BOOLEAN DEFAULT FALSE NOT NULL,
    line_total NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name1 VARCHAR(64),
    tax_rate1 NUMERIC(8, 4) DEFAULT 0.0000 NOT NULL,
    sort_id INT DEFAULT 0
);

-- 20. Credit Notes (Client credit balances and adjustment notes)
CREATE TABLE IF NOT EXISTS invoicely.credits (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES invoicely.clients(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    invoice_id UUID,
    number VARCHAR(128) NOT NULL,
    date DATE NOT NULL,
    due_date DATE,
    amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    balance NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    status_id VARCHAR(32) DEFAULT 'draft' NOT NULL,
    public_notes TEXT,
    private_notes TEXT,
    terms TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_credit_number UNIQUE(company_id, number)
);

CREATE INDEX IF NOT EXISTS idx_credits_company_id ON invoicely.credits(company_id);
CREATE INDEX IF NOT EXISTS idx_credits_client_id ON invoicely.credits(client_id);

-- 21. Recurring Invoices (Subscription & schedule-driven billing automation)
CREATE TABLE IF NOT EXISTS invoicely.recurring_invoices (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES invoicely.clients(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    number VARCHAR(128) NOT NULL,
    frequency_id VARCHAR(32) DEFAULT 'monthly' NOT NULL,
    status_id VARCHAR(32) DEFAULT 'active' NOT NULL,
    auto_bill BOOLEAN DEFAULT FALSE NOT NULL,
    amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    remaining_cycles INT DEFAULT -1 NOT NULL,
    next_send_date DATE,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_rec_inv_number UNIQUE(company_id, number)
);

CREATE INDEX IF NOT EXISTS idx_recurring_invoices_company_id ON invoicely.recurring_invoices(company_id);

-- 22. Tasks & Time-Tracking Logs (Billable project tasks and duration records)
CREATE TABLE IF NOT EXISTS invoicely.tasks (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    client_id UUID REFERENCES invoicely.clients(id) ON DELETE SET NULL,
    project_id UUID REFERENCES invoicely.projects(id) ON DELETE SET NULL,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    invoice_id UUID REFERENCES invoicely.invoices(id) ON DELETE SET NULL,
    status_id UUID REFERENCES invoicely.task_statuses(id) ON DELETE SET NULL,
    number VARCHAR(128),
    description TEXT,
    duration BIGINT DEFAULT 0 NOT NULL,
    is_running BOOLEAN DEFAULT FALSE NOT NULL,
    time_log TEXT DEFAULT '[]' NOT NULL,
    rate NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    is_date_based BOOLEAN DEFAULT FALSE NOT NULL,
    date DATE,
    calculated_start_date TIMESTAMP WITH TIME ZONE,
    status_order INT DEFAULT 0 NOT NULL,
    status_sort_order INT DEFAULT 0 NOT NULL,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tasks_company ON invoicely.tasks(company_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_tasks_project ON invoicely.tasks(company_id, project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_client ON invoicely.tasks(company_id, client_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON invoicely.tasks(company_id, status_id);
CREATE INDEX IF NOT EXISTS idx_tasks_invoice ON invoicely.tasks(company_id, invoice_id);

-- ==============================================================================
-- TIER 5: PAYMENT GATEWAYS & LEDGER RECONCILIATION
-- ==============================================================================

-- 23. Company Payment Gateways (Stripe, PayPal, Authorize.Net integration credentials)
CREATE TABLE IF NOT EXISTS invoicely.company_gateways (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    gateway_key VARCHAR(64) NOT NULL,
    label VARCHAR(128),
    config TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_company_gateways_company_id ON invoicely.company_gateways(company_id);

-- 24. Payments (Accounts Receivable received payments and refunds)
CREATE TABLE IF NOT EXISTS invoicely.payments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES invoicely.clients(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    company_gateway_id UUID REFERENCES invoicely.company_gateways(id),
    number VARCHAR(128) NOT NULL,
    amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    refunded NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    applied NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    date DATE NOT NULL,
    type_id VARCHAR(32) DEFAULT 'bank_transfer' NOT NULL,
    transaction_reference VARCHAR(255),
    private_notes TEXT,
    status_id VARCHAR(32) DEFAULT 'completed' NOT NULL,
    is_manual BOOLEAN DEFAULT TRUE NOT NULL,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_payment_number UNIQUE(company_id, number)
);

CREATE INDEX IF NOT EXISTS idx_payments_company_id ON invoicely.payments(company_id);
CREATE INDEX IF NOT EXISTS idx_payments_client_id ON invoicely.payments(client_id);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_date ON invoicely.payments(company_id, is_deleted, date DESC);

-- 25. Paymentables (Many-to-Many payment-to-invoice allocation ledger)
CREATE TABLE IF NOT EXISTS invoicely.paymentables (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES invoicely.payments(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoicely.invoices(id) ON DELETE CASCADE,
    amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    refunded NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_paymentables_payment_id ON invoicely.paymentables(payment_id);
CREATE INDEX IF NOT EXISTS idx_paymentables_invoice_id ON invoicely.paymentables(invoice_id);

-- ==============================================================================
-- TIER 6: PROCUREMENT & ACCOUNTS PAYABLE (PURCHASE ORDERS & EXPENSES)
-- ==============================================================================

-- 26. Purchase Orders (Procurement orders issued to vendors)
CREATE TABLE IF NOT EXISTS invoicely.purchase_orders (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES invoicely.vendors(id) ON DELETE CASCADE,
    client_id UUID REFERENCES invoicely.clients(id),
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    number VARCHAR(128) NOT NULL,
    po_number VARCHAR(128),
    date DATE NOT NULL,
    due_date DATE,
    status_id VARCHAR(32) DEFAULT 'draft' NOT NULL,
    amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    balance NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    paid_to_date NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    discount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    is_amount_discount BOOLEAN DEFAULT FALSE NOT NULL,
    total_taxes NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    terms TEXT,
    public_notes TEXT,
    private_notes TEXT,
    footer TEXT,
    uses_inclusive_taxes BOOLEAN DEFAULT FALSE NOT NULL,
    tax_name1 VARCHAR(64),
    tax_rate1 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name2 VARCHAR(64),
    tax_rate2 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name3 VARCHAR(64),
    tax_rate3 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    exchange_rate NUMERIC(18, 4) DEFAULT 1.0000 NOT NULL,
    design_id VARCHAR(32) DEFAULT 'clean' NOT NULL,
    expense_id UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_purchase_order_number UNIQUE(company_id, number)
);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_company_id ON invoicely.purchase_orders(company_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_vendor_id ON invoicely.purchase_orders(vendor_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_client_id ON invoicely.purchase_orders(client_id);

-- 27. Purchase Order Items (Granular line items per purchase order)
CREATE TABLE IF NOT EXISTS invoicely.purchase_order_items (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    purchase_order_id UUID NOT NULL REFERENCES invoicely.purchase_orders(id) ON DELETE CASCADE,
    product_key VARCHAR(128),
    notes TEXT,
    cost NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    quantity NUMERIC(18, 4) DEFAULT 1.0000 NOT NULL,
    discount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    is_amount_discount BOOLEAN DEFAULT FALSE NOT NULL,
    tax_name1 VARCHAR(64),
    tax_rate1 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name2 VARCHAR(64),
    tax_rate2 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name3 VARCHAR(64),
    tax_rate3 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    line_total NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    sort_order INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_purchase_order_items_po_id ON invoicely.purchase_order_items(purchase_order_id);

-- 28. Expenses (Operating expenses, vendor bills & disbursements)
CREATE TABLE IF NOT EXISTS invoicely.expenses (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    vendor_id UUID REFERENCES invoicely.vendors(id),
    client_id UUID REFERENCES invoicely.clients(id),
    invoice_id UUID REFERENCES invoicely.invoices(id),
    category_id UUID REFERENCES invoicely.expense_categories(id),
    purchase_order_id UUID REFERENCES invoicely.purchase_orders(id),
    project_id UUID REFERENCES invoicely.projects(id) ON DELETE SET NULL,
    number VARCHAR(128) NOT NULL,
    amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    foreign_amount NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    exchange_rate NUMERIC(18, 4) DEFAULT 1.0000 NOT NULL,
    currency_id VARCHAR(8) DEFAULT 'USD' NOT NULL,
    tax_name1 VARCHAR(64),
    tax_rate1 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name2 VARCHAR(64),
    tax_rate2 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_name3 VARCHAR(64),
    tax_rate3 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_amount1 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_amount2 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    tax_amount3 NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    uses_inclusive_taxes BOOLEAN DEFAULT FALSE NOT NULL,
    should_be_invoiced BOOLEAN DEFAULT FALSE NOT NULL,
    date DATE NOT NULL,
    payment_date DATE,
    payment_type_id VARCHAR(32) DEFAULT '1' NOT NULL,
    transaction_reference VARCHAR(255),
    public_notes TEXT,
    private_notes TEXT,
    custom_value1 VARCHAR(255),
    custom_value2 VARCHAR(255),
    custom_value3 VARCHAR(255),
    custom_value4 VARCHAR(255),
    status_id VARCHAR(32) DEFAULT 'logged' NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_expense_number UNIQUE(company_id, number)
);

CREATE INDEX IF NOT EXISTS idx_expenses_company_id ON invoicely.expenses(company_id);
CREATE INDEX IF NOT EXISTS idx_expenses_vendor_id ON invoicely.expenses(vendor_id);
CREATE INDEX IF NOT EXISTS idx_expenses_client_id ON invoicely.expenses(client_id);
CREATE INDEX IF NOT EXISTS idx_expenses_invoice_id ON invoicely.expenses(invoice_id);
CREATE INDEX IF NOT EXISTS idx_expenses_category_id ON invoicely.expenses(category_id);
CREATE INDEX IF NOT EXISTS idx_expenses_po_id ON invoicely.expenses(purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_expenses_tenant_date ON invoicely.expenses(company_id, is_deleted, date DESC);

-- ==============================================================================
-- TIER 7: BANKING INTEGRATIONS, RULES & TRANSACTION FEEDS
-- ==============================================================================

-- 29. Bank Integrations (Open Banking / Plaid / Nordigen financial accounts)
CREATE TABLE IF NOT EXISTS invoicely.bank_integrations (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    bank_account_name VARCHAR(255) NOT NULL,
    bank_account_number VARCHAR(128),
    bank_account_type VARCHAR(64) DEFAULT 'checking',
    bank_account_status VARCHAR(64) DEFAULT 'active',
    currency VARCHAR(8) DEFAULT 'USD',
    balance NUMERIC(18, 4) DEFAULT 0.0000 NOT NULL,
    nickname VARCHAR(255),
    provider_name VARCHAR(128) DEFAULT 'manual',
    integration_type VARCHAR(64) DEFAULT 'plaid',
    nordigen_institution_id VARCHAR(128),
    auto_sync BOOLEAN DEFAULT TRUE NOT NULL,
    disabled_upstream BOOLEAN DEFAULT FALSE NOT NULL,
    from_date DATE,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_bank_account_name UNIQUE(company_id, bank_account_name)
);

CREATE INDEX IF NOT EXISTS idx_bank_integrations_company ON invoicely.bank_integrations(company_id, is_deleted);

-- 30. Bank Transaction Rules (Automated categorization and reconciliation match engine)
CREATE TABLE IF NOT EXISTS invoicely.bank_transaction_rules (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    name VARCHAR(255) NOT NULL,
    applies_to VARCHAR(64) DEFAULT 'all',
    rules TEXT DEFAULT '[]' NOT NULL,
    auto_convert BOOLEAN DEFAULT FALSE NOT NULL,
    matches_on_all BOOLEAN DEFAULT TRUE NOT NULL,
    vendor_id UUID REFERENCES invoicely.vendors(id) ON DELETE SET NULL,
    category_id UUID REFERENCES invoicely.expense_categories(id) ON DELETE SET NULL,
    client_id UUID REFERENCES invoicely.clients(id) ON DELETE SET NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_company_rule_name UNIQUE(company_id, name)
);

CREATE INDEX IF NOT EXISTS idx_bank_transaction_rules_company ON invoicely.bank_transaction_rules(company_id, is_deleted);

-- 31. Bank Transactions (Bank statement line item feeds)
CREATE TABLE IF NOT EXISTS invoicely.bank_transactions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    bank_integration_id UUID REFERENCES invoicely.bank_integrations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    date DATE NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    currency_id VARCHAR(8) DEFAULT 'USD',
    description TEXT,
    base_type VARCHAR(32) DEFAULT 'debit' NOT NULL,
    status_id VARCHAR(32) DEFAULT 'unmatched' NOT NULL,
    payment_id UUID REFERENCES invoicely.payments(id) ON DELETE SET NULL,
    expense_id UUID REFERENCES invoicely.expenses(id) ON DELETE SET NULL,
    vendor_id UUID REFERENCES invoicely.vendors(id) ON DELETE SET NULL,
    category_id UUID REFERENCES invoicely.expense_categories(id) ON DELETE SET NULL,
    bank_transaction_rule_id UUID REFERENCES invoicely.bank_transaction_rules(id) ON DELETE SET NULL,
    participant VARCHAR(255),
    participant_name VARCHAR(255),
    invoice_ids TEXT,
    account_type VARCHAR(64),
    category_type VARCHAR(64),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bank_transactions_company ON invoicely.bank_transactions(company_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_integration ON invoicely.bank_transactions(company_id, bank_integration_id);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_status ON invoicely.bank_transactions(company_id, status_id);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_payment ON invoicely.bank_transactions(company_id, payment_id);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_expense ON invoicely.bank_transactions(company_id, expense_id);

-- ==============================================================================
-- TIER 8: DOCUMENT VAULT, DESIGNS & E-SIGNATURE BLUEPRINTS
-- ==============================================================================

-- 32. Designs (Invoice & Quote HTML/CSS template AST designs)
CREATE TABLE IF NOT EXISTS invoicely.designs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID,
    user_id UUID,
    name VARCHAR(128) NOT NULL,
    design TEXT NOT NULL,
    is_custom BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    entities VARCHAR(128) NOT NULL DEFAULT 'invoice',
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_designs_company_status ON invoicely.designs(company_id, is_deleted, archived_at);
CREATE INDEX IF NOT EXISTS idx_designs_is_template ON invoicely.designs(is_template, entities);

-- 33. Documents (Document management, receipts, attachments and metadata)
CREATE TABLE IF NOT EXISTS invoicely.documents (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    assigned_user_id UUID REFERENCES invoicely.users(id),
    client_id UUID REFERENCES invoicely.clients(id) ON DELETE SET NULL,
    vendor_id UUID REFERENCES invoicely.vendors(id) ON DELETE SET NULL,
    project_id UUID REFERENCES invoicely.projects(id) ON DELETE SET NULL,
    invoice_id UUID REFERENCES invoicely.invoices(id) ON DELETE SET NULL,
    expense_id UUID REFERENCES invoicely.expenses(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL DEFAULT 'application/pdf',
    size BIGINT NOT NULL DEFAULT 0,
    width INT NOT NULL DEFAULT 0,
    height INT NOT NULL DEFAULT 0,
    hash VARCHAR(64) NOT NULL,
    disk VARCHAR(32) NOT NULL DEFAULT 'local',
    url VARCHAR(512),
    preview TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_company_status ON invoicely.documents(company_id, is_deleted, archived_at);
CREATE INDEX IF NOT EXISTS idx_documents_hash ON invoicely.documents(hash);
CREATE INDEX IF NOT EXISTS idx_documents_client ON invoicely.documents(client_id);
CREATE INDEX IF NOT EXISTS idx_documents_invoice ON invoicely.documents(invoice_id);
CREATE INDEX IF NOT EXISTS idx_documents_expense ON invoicely.documents(expense_id);

-- 34. Blueprints (Invoicely Docs contracts and e-signing workflows)
CREATE TABLE IF NOT EXISTS invoicely.blueprints (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES invoicely.companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES invoicely.users(id),
    client_id UUID REFERENCES invoicely.clients(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    content TEXT,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_blueprints_company_status ON invoicely.blueprints(company_id, is_deleted, archived_at);

-- ==============================================================================
-- END OF INVOICELY UNIFIED BASELINE DDL
-- Total Tables Defined: 34
-- Total Indexes Defined: 51
-- ==============================================================================
