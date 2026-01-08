-- Migration: Insert initial data (EntityTypes, Actions, UIBindings)
-- Created: 2026-01-07

-- Insert Entity Types
INSERT INTO entity_types (id, name, created_at, updated_at) VALUES
    ('Building', 'Здание', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Contract', 'Договор', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Actions
INSERT INTO actions (id, name, description, created_at, updated_at) VALUES
    ('order_egrn_extract', 'Заказать выписку из ЕГРН', 'Заказывает выписку из ЕГРН для указанного здания', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('close_contract', 'Закрыть договор', 'Закрывает указанный договор', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('assign_owner', 'Назначить владельца', 'Назначает владельца для указанного здания', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Action Applicable Entity Types
INSERT INTO action_applicable_entity_types (action_id, entity_type_id) VALUES
    ('order_egrn_extract', 'Building'),
    ('close_contract', 'Contract'),
    ('assign_owner', 'Building');

-- Insert UI Bindings
INSERT INTO ui_bindings (action_id, selector, selector_type, created_at, updated_at) VALUES
    ('order_egrn_extract', '[data-action=''order_egrn_extract'']', 'CSS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('close_contract', '//button[contains(@class, ''close-contract-btn'')]', 'XPATH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('assign_owner', '[data-action=''assign_owner'']', 'CSS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
