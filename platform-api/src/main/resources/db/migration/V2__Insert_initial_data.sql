-- Migration: Insert initial data по схеме drawio (zbrstk.entity_type, system.action, system.ui_binding)

-- Entity types (zbrstk.entity_type: shortname, displayname, created_time, updated_time)
INSERT INTO zbrstk.entity_type (shortname, displayname, created_time, updated_time) VALUES
    ('Building', 'Здание', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Contract', 'Договор', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Actions (system.action: shortname, displayname, description, created_time, updated_time)
INSERT INTO system.action (shortname, displayname, description, created_time, updated_time) VALUES
    ('order_egrn_extract', 'Заказать выписку из ЕГРН', 'Заказывает выписку из ЕГРН для указанного здания', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('close_contract', 'Закрыть договор', 'Закрывает указанный договор', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('assign_owner', 'Назначить владельца', 'Назначает владельца для указанного здания', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Action applicable entity types (system.action_applicable_entity_type: action, entity_type)
INSERT INTO system.action_applicable_entity_type (action, entity_type) VALUES
    ('order_egrn_extract', 'Building'),
    ('close_contract', 'Contract'),
    ('assign_owner', 'Building');

-- UI Bindings (system.ui_binding: action, selector, selector_type, created_time, updated_time)
INSERT INTO system.ui_binding (action, selector, selector_type, created_time, updated_time) VALUES
    ('order_egrn_extract', '[data-action=''order_egrn_extract'']', 'CSS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('close_contract', '//button[contains(@class, ''close-contract-btn'')]', 'XPATH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('assign_owner', '[data-action=''assign_owner'']', 'CSS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
