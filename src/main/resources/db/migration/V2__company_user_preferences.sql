-- ==============================================================================
-- Invoicely Platform - Migration V2: Company User Preferences & Settings
-- ==============================================================================
-- Adds preferences JSONB store to company_users to support UI react_settings
-- (theme colors, dark mode, keyboard shortcuts, table preferences, etc.)
-- ==============================================================================

ALTER TABLE invoicely.company_users
ADD COLUMN IF NOT EXISTS preferences JSONB DEFAULT '{}'::jsonb NOT NULL;

COMMENT ON COLUMN invoicely.company_users.preferences IS 'Per-user per-tenant UI preferences (react_settings: dark mode, density, custom shortcuts)';
