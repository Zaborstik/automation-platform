-- ========== СХЕМЫ ==========
CREATE SCHEMA IF NOT EXISTS zbrtstk;
CREATE SCHEMA IF NOT EXISTS system;

-- entity_type: Типы сущностей (system — по JPA и ссылкам plan_step, action_applicable_entity_type)
CREATE TABLE IF NOT EXISTS system.entity_type (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    km_article VARCHAR(36),
    ui_description TEXT,
    entityfieldlist VARCHAR(10000),
    buttons VARCHAR(10000)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_entity_type_id_uniq ON system.entity_type(id);
--
-- -- entity_type_metadata: Метаданные типа (ключ-значение)
-- CREATE TABLE IF NOT EXISTS zbrtstk.entity_type_metadata (
--     id VARCHAR(36) NOT NULL PRIMARY KEY,
--     entity_type VARCHAR(36) NOT NULL,
--     meta_key VARCHAR(36) NOT NULL,
--     meta_value TEXT,
--     FOREIGN KEY (entity_type) REFERENCES zbrtstk.entity_type(id)
-- );

--
-- -- ui_binding: Привязка действия к UI (селектор)
-- CREATE TABLE IF NOT EXISTS system.ui_binding ( -- не финальные, тут хз
--     action VARCHAR(36) NOT NULL PRIMARY KEY,
--     selector VARCHAR(510) NOT NULL,
--     selector_type VARCHAR(36) NOT NULL,
--     created_time TIMESTAMP NOT NULL,
--     updated_time TIMESTAMP NOT NULL,
--     FOREIGN KEY (action) REFERENCES system.action(id)
--     );

-- -- ui_binding_metadata: Метаданные привязки к UI
-- CREATE TABLE IF NOT EXISTS system.ui_binding_metadata ( -- не финальные, тут хз
--     ui_binding VARCHAR(36) NOT NULL,
--     meta_key VARCHAR(36) NOT NULL,
--     meta_value TEXT,
--     PRIMARY KEY (ui_binding, meta_key),
--     FOREIGN KEY (ui_binding) REFERENCES system.ui_binding(action)
-- );

-- workflow_step: Шаг ЖЦ
CREATE TABLE IF NOT EXISTS system.workflow_step (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    internalname VARCHAR(255) NOT NULL, -- имя на английском, типа new in_progres
    displayname VARCHAR(255) NOT NULL, -- отображаемое пользователю имя
    sortorder INT -- номер по счёту
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_workflow_step_id_uniq ON system.workflow_step(id);
CREATE INDEX IF NOT EXISTS idx_workflow_step_sortorder ON system.workflow_step(sortorder);

-- workflow: ЖЦ
CREATE TABLE IF NOT EXISTS system.workflow (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255) NOT NULL, -- отображаемое пользователю имя
    firststep VARCHAR(36) NOT NULL, -- первый шаг
    FOREIGN KEY (firststep) REFERENCES system.workflow_step(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_workflow_id_uniq ON system.workflow(id);
CREATE INDEX IF NOT EXISTS idx_workflow_displayname ON system.workflow(displayname);

-- attachment: Вложение
CREATE TABLE IF NOT EXISTS zbrtstk.attachment (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255) -- отображаемое пользователю имя
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_attachment_id_uniq ON zbrtstk.attachment(id);
CREATE INDEX IF NOT EXISTS idx_attachment_displayname ON zbrtstk.attachment(displayname);

-- plan: План выполнения
CREATE TABLE IF NOT EXISTS zbrtstk.plan (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow VARCHAR(36) NOT NULL,
    workflow_step_internalname VARCHAR(255) NOT NULL, -- шаг ЖЦ
    stopped_at_plan_step VARCHAR(36) NOT NULL, -- остановился на шаге плана
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP,
    target VARCHAR(510), -- цель, пока хз нужно ли это поле
    explanation VARCHAR(1020), -- инфа для пользователя
    FOREIGN KEY (workflow) REFERENCES system.workflow(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_plan_id_uniq ON zbrtstk.plan(id);

-- plan_step: Шаги плана
CREATE TABLE IF NOT EXISTS zbrtstk.plan_step (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    plan VARCHAR(36) NOT NULL,
    workflow VARCHAR(36) NOT NULL,
    workflow_step_internalname VARCHAR(255) NOT NULL, -- шаг ЖЦ
    entitytype VARCHAR(36) NOT NULL,
    entity_id VARCHAR(510), -- URL/селекторы и id объекта (до 510 символов)
    sortorder INT NOT NULL, -- порядковый номер шага
    displayname VARCHAR(255) NOT NULL, -- отображаемое пользователю имя
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    FOREIGN KEY (plan) REFERENCES zbrtstk.plan(id),
    FOREIGN KEY (workflow) REFERENCES system.workflow(id),
    FOREIGN KEY (entitytype) REFERENCES system.entity_type(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_plan_step_id_uniq ON zbrtstk.plan_step(id);
CREATE INDEX IF NOT EXISTS idx_plan_step_plan ON zbrtstk.plan_step(plan);
CREATE INDEX IF NOT EXISTS idx_plan_step_sortorder ON zbrtstk.plan_step(sortorder);

-- action_type: Типы действия (создаём до plan_step_action, т.к. action ссылается на action_type)
CREATE TABLE IF NOT EXISTS system.action_type (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    internalname VARCHAR(255) NOT NULL, -- имя на английском, типа open clic
    displayname VARCHAR(255) NOT NULL -- отображаемое пользователю имя
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_action_type_id_uniq ON system.action_type(id);

-- action: Действия платформы (создаём до plan_step_action)
CREATE TABLE IF NOT EXISTS system.action (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255) NOT NULL, -- отображаемое пользователю имя
    internalname VARCHAR(255) NOT NULL,
    meta_value TEXT,
    description VARCHAR(255), -- описание для пользователя
    action_type VARCHAR(36) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    FOREIGN KEY (action_type) REFERENCES system.action_type(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_action_id_uniq ON system.action(id);
CREATE INDEX IF NOT EXISTS idx_action_action_type ON system.action(action_type);

-- action_applicable_entity_type: Действия, применимые к объекту дочерней системы
CREATE TABLE IF NOT EXISTS system.action_applicable_entity_type (
    action VARCHAR(36) NOT NULL,
    entity_type VARCHAR(36) NOT NULL, -- тип объекта
    PRIMARY KEY (action, entity_type),
    FOREIGN KEY (action) REFERENCES system.action(id),
    FOREIGN KEY (entity_type) REFERENCES system.entity_type(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_action_applicable_entity_type_uniq ON system.action_applicable_entity_type(action, entity_type);

-- plan_step_action: связка шаг - действия
CREATE TABLE IF NOT EXISTS zbrtstk.plan_step_action (
    plan_step VARCHAR(36) NOT NULL,
    action VARCHAR(36) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (plan_step, action),
    FOREIGN KEY (plan_step) REFERENCES zbrtstk.plan_step(id),
    FOREIGN KEY (action) REFERENCES system.action(id)
);
CREATE INDEX IF NOT EXISTS idx_plan_step_action_plan_step ON zbrtstk.plan_step_action(plan_step);
CREATE UNIQUE INDEX IF NOT EXISTS idx_plan_step_action_plan_step_action_uniq ON zbrtstk.plan_step_action(plan_step, action);


-- execution_result: Итог выполнения плана
CREATE TABLE IF NOT EXISTS zbrtstk.plan_result (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    plan VARCHAR(36) NOT NULL,
    success BOOLEAN NOT NULL,
    started_time TIMESTAMP NOT NULL,
    finished_time TIMESTAMP NOT NULL,
    FOREIGN KEY (plan) REFERENCES zbrtstk.plan(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_plan_result_id_uniq ON zbrtstk.plan_result(id);
CREATE INDEX IF NOT EXISTS idx_plan_result_plan ON zbrtstk.plan_result(plan);


-- execution_log_entry: Лог по шагам
CREATE TABLE IF NOT EXISTS zbrtstk.plan_step_log_entry ( -- создаётся в случае падения
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    plan VARCHAR(36) NOT NULL,
    plan_step VARCHAR(36) NOT NULL,
    plan_result VARCHAR(36) NOT NULL,
    action VARCHAR(36) NOT NULL,
    message VARCHAR(510) NOT NULL, -- сообщение которое написал пользователь
    error VARCHAR(2000), -- сообщение об ошибке (Playwright и др.)
    executed_time TIMESTAMP NOT NULL,
    execution_time_ms BIGINT,
    attachment VARCHAR(36), -- вложение, мб скрин, либо что-то такое, а мб в будущем сделать несколько вложений
    FOREIGN KEY (plan) REFERENCES zbrtstk.plan(id),
    FOREIGN KEY (plan_step) REFERENCES zbrtstk.plan_step(id),
    FOREIGN KEY (plan_result) REFERENCES zbrtstk.plan_result(id),
    FOREIGN KEY (action) REFERENCES system.action(id),
    FOREIGN KEY (attachment) REFERENCES zbrtstk.attachment(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_plan_step_log_entry_id_uniq ON zbrtstk.plan_step_log_entry(id);
CREATE INDEX IF NOT EXISTS idx_plan_step_log_entry_plan ON zbrtstk.plan_step_log_entry(plan);

-- scenario / scenario_step / scenario_step_action: шаблоны планов
CREATE TABLE IF NOT EXISTS zbrtstk.scenario (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    target VARCHAR(510),
    explanation VARCHAR(1020),
    workflow VARCHAR(36) NOT NULL,
    workflow_step_internalname VARCHAR(255) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP,
    FOREIGN KEY (workflow) REFERENCES system.workflow(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_scenario_id_uniq ON zbrtstk.scenario(id);
CREATE INDEX IF NOT EXISTS idx_scenario_name ON zbrtstk.scenario(name);

CREATE TABLE IF NOT EXISTS zbrtstk.scenario_step (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    scenario VARCHAR(36) NOT NULL,
    workflow VARCHAR(36) NOT NULL,
    workflow_step_internalname VARCHAR(255) NOT NULL,
    entitytype VARCHAR(36) NOT NULL,
    entity_id VARCHAR(510),
    sortorder INT NOT NULL,
    displayname VARCHAR(255) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    FOREIGN KEY (scenario) REFERENCES zbrtstk.scenario(id),
    FOREIGN KEY (workflow) REFERENCES system.workflow(id),
    FOREIGN KEY (entitytype) REFERENCES system.entity_type(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_scenario_step_id_uniq ON zbrtstk.scenario_step(id);
CREATE INDEX IF NOT EXISTS idx_scenario_step_scenario ON zbrtstk.scenario_step(scenario);
CREATE INDEX IF NOT EXISTS idx_scenario_step_sortorder ON zbrtstk.scenario_step(sortorder);

CREATE TABLE IF NOT EXISTS zbrtstk.scenario_step_action (
    scenario_step VARCHAR(36) NOT NULL,
    action VARCHAR(36) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (scenario_step, action),
    FOREIGN KEY (scenario_step) REFERENCES zbrtstk.scenario_step(id),
    FOREIGN KEY (action) REFERENCES system.action(id)
);
CREATE INDEX IF NOT EXISTS idx_scenario_step_action_step ON zbrtstk.scenario_step_action(scenario_step);

-- workflow_transition: допустимые переходы между шагами ЖЦ
CREATE TABLE IF NOT EXISTS system.workflow_transition (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow VARCHAR(36) NOT NULL,
    from_step VARCHAR(255) NOT NULL,
    to_step VARCHAR(255) NOT NULL,
    FOREIGN KEY (workflow) REFERENCES system.workflow(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_workflow_transition_uniq
    ON system.workflow_transition (workflow, from_step, to_step);


-- -- Универсальная таблица entities: одна таблица для всех сущностей (table_name + id + data JSON).
-- CREATE TABLE IF NOT EXISTS system.entities (
--     table_name VARCHAR(255) NOT NULL,
--     id VARCHAR(255) NOT NULL,
--     data CLOB,
--     created_at TIMESTAMP,
--     updated_at TIMESTAMP,
--     PRIMARY KEY (table_name, id)
-- );

-- CREATE INDEX IF NOT EXISTS idx_entities_table_name ON system.entities(table_name);
-- CREATE INDEX IF NOT EXISTS idx_entities_table_id ON system.entities(table_name, id);

-- -- Первичные данные: entity_types, actions, ui_bindings в формате JSON (для работы через EntityDTO).
-- INSERT INTO system.entities (table_name, id, data, created_at, updated_at) VALUES
--     ('entity_types', 'Building', '{"name":"Здание","metadata":{}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     ('entity_types', 'Contract', '{"name":"Договор","metadata":{}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     ('actions', 'order_egrn_extract', '{"name":"Заказать выписку из ЕГРН","description":"Заказывает выписку из ЕГРН для указанного здания","applicableEntityTypes":["Building"],"metadata":{}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     ('actions', 'close_contract', '{"name":"Закрыть договор","description":"Закрывает указанный договор","applicableEntityTypes":["Contract"],"metadata":{}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     ('actions', 'assign_owner', '{"name":"Назначить владельца","description":"Назначает владельца для указанного здания","applicableEntityTypes":["Building"],"metadata":{}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     ('ui_bindings', 'order_egrn_extract', '{"selector":"[data-action=''order_egrn_extract'']","selectorType":"CSS","metadata":{}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     ('ui_bindings', 'close_contract', '{"selector":"//button[contains(@class, ''close-contract-btn'')]","selectorType":"XPATH","metadata":{}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
--     ('ui_bindings', 'assign_owner', '{"selector":"[data-action=''assign_owner'']","selectorType":"CSS","metadata":{}}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
