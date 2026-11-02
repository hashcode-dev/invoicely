-- ==============================================================================
-- Invoicely Platform - Migration V2: Performance Indexes & Relational Sequences
-- ==============================================================================

CREATE TABLE IF NOT EXISTS invoicely.system_sequences (
    id VARCHAR(128) PRIMARY KEY,
    seq BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Multi-tenant composite B-Tree indexes for high-throughput query workloads
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status_date 
    ON invoicely.invoices (company_id, is_deleted, status_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_invoices_tenant_client 
    ON invoicely.invoices (company_id, client_id, due_date);

CREATE INDEX IF NOT EXISTS idx_payments_tenant_date 
    ON invoicely.payments (company_id, is_deleted, date DESC);

CREATE INDEX IF NOT EXISTS idx_expenses_tenant_date 
    ON invoicely.expenses (company_id, is_deleted, date DESC);
