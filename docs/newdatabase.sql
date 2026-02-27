-- Схема БД по docs/newdatabase.drawio
-- Бизнес-логика: схема zbrtstk. Всё для работы приложения: схема system.
-- UUID = VARCHAR(36). VJ = VARCHAR(10000).

-- ========== СХЕМЫ ==========
CREATE SCHEMA IF NOT EXISTS zbrtstk;
CREATE SCHEMA IF NOT EXISTS system;

-- ========== ZBRTSTK (бизнес-логика) ==========

CREATE TABLE IF NOT EXISTS zbrtstk.entity_type (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    km_article VARCHAR(36),
    ui_description TEXT,
    entityfieldlist VARCHAR(10000),
    buttons VARCHAR(10000)
);

CREATE TABLE IF NOT EXISTS zbrtstk.entity_type_metadata (
    entity_type VARCHAR(36) NOT NULL,
    meta_key VARCHAR(36) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (entity_type, meta_key),
    FOREIGN KEY (entity_type) REFERENCES zbrtstk.entity_type(shortname) ON DELETE CASCADE
);

-- ========== SYSTEM (платформа) ==========

CREATE TABLE IF NOT EXISTS system.step_type (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS system.workflow_step (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255),
    internalname VARCHAR(255),
    sortorder INT
);

CREATE TABLE IF NOT EXISTS system.workflow (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255),
    firststep VARCHAR(36),
    FOREIGN KEY (firststep) REFERENCES system.workflow_step(shortname)
);

CREATE TABLE IF NOT EXISTS system.action (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255) NOT NULL,
    description TEXT,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS system.action_applicable_entity_type (
    action VARCHAR(36) NOT NULL,
    entity_type VARCHAR(36) NOT NULL,
    PRIMARY KEY (action, entity_type),
    FOREIGN KEY (action) REFERENCES system.action(shortname) ON DELETE CASCADE,
    FOREIGN KEY (entity_type) REFERENCES zbrtstk.entity_type(shortname) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system.action_metadata (
    action VARCHAR(36) NOT NULL,
    meta_key VARCHAR(36) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (action, meta_key),
    FOREIGN KEY (action) REFERENCES system.action(shortname) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system.ui_binding (
    action VARCHAR(36) NOT NULL PRIMARY KEY,
    selector VARCHAR(1000) NOT NULL,
    selector_type VARCHAR(50) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    FOREIGN KEY (action) REFERENCES system.action(shortname) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system.ui_binding_metadata (
    ui_binding VARCHAR(36) NOT NULL,
    meta_key VARCHAR(36) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (ui_binding, meta_key),
    FOREIGN KEY (ui_binding) REFERENCES system.ui_binding(action) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system.attachment (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    displayname VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS system.plan (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    action VARCHAR(36) NOT NULL,
    workflow VARCHAR(36),
    workflowstepname VARCHAR(255),
    stopped_at_step VARCHAR(255),
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    entity_type_id VARCHAR(36),
    entity_id VARCHAR(255),
    status VARCHAR(50),
    FOREIGN KEY (action) REFERENCES system.action(shortname),
    FOREIGN KEY (workflow) REFERENCES system.workflow(shortname),
    FOREIGN KEY (entity_type_id) REFERENCES zbrtstk.entity_type(shortname)
);

CREATE TABLE IF NOT EXISTS system.plan_step (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    plan VARCHAR(36) NOT NULL,
    workflow VARCHAR(36),
    workflowstepname VARCHAR(255),
    entitytype VARCHAR(36),
    entity_shortname VARCHAR(36),
    sortorder INT NOT NULL,
    step_type VARCHAR(36),
    target VARCHAR(1000),
    explanation VARCHAR(2000),
    displayname VARCHAR(255),
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    FOREIGN KEY (plan) REFERENCES system.plan(shortname) ON DELETE CASCADE,
    FOREIGN KEY (workflow) REFERENCES system.workflow(shortname),
    FOREIGN KEY (entitytype) REFERENCES zbrtstk.entity_type(shortname),
    FOREIGN KEY (step_type) REFERENCES system.step_type(shortname)
);

CREATE TABLE IF NOT EXISTS system.plan_step_parameter (
    plan_step VARCHAR(36) NOT NULL,
    meta_key VARCHAR(36) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (plan_step, meta_key),
    FOREIGN KEY (plan_step) REFERENCES system.plan_step(shortname) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system.execution_results (
    shortname VARCHAR(36) NOT NULL PRIMARY KEY,
    plan VARCHAR(36) NOT NULL UNIQUE,
    success BOOLEAN NOT NULL,
    started_time TIMESTAMP NOT NULL,
    finished_time TIMESTAMP,
    created_time TIMESTAMP NOT NULL,
    FOREIGN KEY (plan) REFERENCES system.plan(shortname)
);

CREATE TABLE IF NOT EXISTS system.execution_log_entries (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    shortname VARCHAR(36) NOT NULL,
    execution_result VARCHAR(36) NOT NULL,
    plan VARCHAR(36) NOT NULL,
    plan_step VARCHAR(36),
    step_type VARCHAR(36),
    message VARCHAR(2000),
    error VARCHAR(2000),
    executed_time TIMESTAMP NOT NULL,
    execution_time_ms BIGINT,
    attachment VARCHAR(36),
    FOREIGN KEY (execution_result) REFERENCES system.execution_results(shortname) ON DELETE CASCADE,
    FOREIGN KEY (plan) REFERENCES system.plan(shortname),
    FOREIGN KEY (plan_step) REFERENCES system.plan_step(shortname),
    FOREIGN KEY (step_type) REFERENCES system.step_type(shortname),
    FOREIGN KEY (attachment) REFERENCES system.attachment(shortname)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_execution_log_entries_shortname ON system.execution_log_entries(shortname);
CREATE INDEX IF NOT EXISTS idx_plan_action ON system.plan(action);
CREATE INDEX IF NOT EXISTS idx_plan_workflow ON system.plan(workflow);
CREATE INDEX IF NOT EXISTS idx_plan_entity_type_id ON system.plan(entity_type_id);
CREATE INDEX IF NOT EXISTS idx_plan_step_plan ON system.plan_step(plan);
CREATE INDEX IF NOT EXISTS idx_plan_step_step_type ON system.plan_step(step_type);
CREATE INDEX IF NOT EXISTS idx_execution_results_plan ON system.execution_results(plan);
CREATE INDEX IF NOT EXISTS idx_execution_log_result ON system.execution_log_entries(execution_result);
CREATE INDEX IF NOT EXISTS idx_execution_log_plan ON system.execution_log_entries(plan);
