-- ==============================================================================
-- Invoicely Platform - Migration V4: SaaS Operator Admin Portal & Support Desk
-- ==============================================================================
-- Supports SaaS Operator Super-Admin operations:
-- 1. Tracking customer organizations, revenue, and active subscription health
-- 2. Customer query resolution (support ticket system with threaded messages)
-- 3. Super-admin audit logging and secure tenant impersonation
-- ==============================================================================

-- 1. Platform Operator Admins
CREATE TABLE IF NOT EXISTS invoicely.platform_admins (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES invoicely.users(id) ON DELETE CASCADE,
    admin_role VARCHAR(32) DEFAULT 'SUPPORT_AGENT' NOT NULL CHECK (admin_role IN ('SUPER_ADMIN', 'FINANCE_OPS', 'SUPPORT_LEAD', 'SUPPORT_AGENT')),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_platform_admin_user UNIQUE(user_id)
);

CREATE INDEX IF NOT EXISTS idx_platform_admins_user ON invoicely.platform_admins(user_id);

-- 2. Customer Support Tickets
CREATE TABLE IF NOT EXISTS invoicely.support_tickets (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_number VARCHAR(32) NOT NULL UNIQUE,
    account_id UUID NOT NULL REFERENCES invoicely.accounts(id) ON DELETE CASCADE,
    company_id UUID REFERENCES invoicely.companies(id) ON DELETE SET NULL,
    reporter_user_id UUID NOT NULL REFERENCES invoicely.users(id),
    assigned_admin_id UUID REFERENCES invoicely.platform_admins(id),
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(32) DEFAULT 'OPEN' NOT NULL CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_ON_CUSTOMER', 'RESOLVED', 'CLOSED')),
    priority VARCHAR(16) DEFAULT 'MEDIUM' NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    category VARCHAR(64) DEFAULT 'GENERAL' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_support_tickets_account ON invoicely.support_tickets(account_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status_priority ON invoicely.support_tickets(status, priority);

-- 3. Support Ticket Messages & Replies
CREATE TABLE IF NOT EXISTS invoicely.support_ticket_messages (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES invoicely.support_tickets(id) ON DELETE CASCADE,
    sender_user_id UUID NOT NULL REFERENCES invoicely.users(id),
    is_internal_note BOOLEAN DEFAULT FALSE NOT NULL,
    message_body TEXT NOT NULL,
    attachments TEXT DEFAULT '[]' NOT NULL, -- JSON array of file URLs
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket ON invoicely.support_ticket_messages(ticket_id, created_at ASC);

-- 4. Platform Super-Admin Audit Logs
CREATE TABLE IF NOT EXISTS invoicely.platform_audit_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES invoicely.users(id),
    action VARCHAR(64) NOT NULL, -- 'IMPERSONATE_TENANT', 'PLAN_OVERRIDE', 'ACCOUNT_LOCK_TOGGLE'
    target_account_id UUID REFERENCES invoicely.accounts(id),
    target_company_id UUID REFERENCES invoicely.companies(id),
    ip_address VARCHAR(45) DEFAULT '127.0.0.1' NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_platform_audit_logs_action ON invoicely.platform_audit_logs(action, created_at DESC);
