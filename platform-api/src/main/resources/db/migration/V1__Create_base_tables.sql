-- Migration: Create base tables for Platform API
-- Created: 2026-01-07
-- Note: Uses PostgreSQL syntax (BIGSERIAL). For H2, Flyway will auto-convert to AUTO_INCREMENT

-- Entity Types table
CREATE TABLE IF NOT EXISTS entity_types (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Entity Type Metadata table
CREATE TABLE IF NOT EXISTS entity_type_metadata (
    entity_type_id VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    value TEXT,
    PRIMARY KEY (entity_type_id, key),
    FOREIGN KEY (entity_type_id) REFERENCES entity_types(id) ON DELETE CASCADE
);

-- Actions table
CREATE TABLE IF NOT EXISTS actions (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Action Applicable Entity Types table
CREATE TABLE IF NOT EXISTS action_applicable_entity_types (
    action_id VARCHAR(255) NOT NULL,
    entity_type_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (action_id, entity_type_id),
    FOREIGN KEY (action_id) REFERENCES actions(id) ON DELETE CASCADE
);

-- Action Metadata table
CREATE TABLE IF NOT EXISTS action_metadata (
    action_id VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    value TEXT,
    PRIMARY KEY (action_id, key),
    FOREIGN KEY (action_id) REFERENCES actions(id) ON DELETE CASCADE
);

-- UI Bindings table
CREATE TABLE IF NOT EXISTS ui_bindings (
    action_id VARCHAR(255) PRIMARY KEY,
    selector VARCHAR(1000) NOT NULL,
    selector_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (action_id) REFERENCES actions(id) ON DELETE CASCADE
);

-- UI Binding Metadata table
CREATE TABLE IF NOT EXISTS ui_binding_metadata (
    action_id VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    value TEXT,
    PRIMARY KEY (action_id, key),
    FOREIGN KEY (action_id) REFERENCES ui_bindings(action_id) ON DELETE CASCADE
);

-- Plans table
CREATE TABLE IF NOT EXISTS plans (
    id VARCHAR(255) PRIMARY KEY,
    entity_type_id VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    action_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (action_id) REFERENCES actions(id)
);

-- Plan Steps table
CREATE TABLE IF NOT EXISTS plan_steps (
    pk BIGSERIAL PRIMARY KEY,
    plan_id VARCHAR(255) NOT NULL,
    step_index INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    target VARCHAR(1000),
    explanation VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE,
    UNIQUE KEY unique_plan_step (plan_id, step_index)
);

-- Plan Step Parameters table
CREATE TABLE IF NOT EXISTS plan_step_parameters (
    step_pk BIGINT NOT NULL,
    key VARCHAR(255) NOT NULL,
    value TEXT,
    PRIMARY KEY (step_pk, key),
    FOREIGN KEY (step_pk) REFERENCES plan_steps(pk) ON DELETE CASCADE
);

-- Execution Results table
CREATE TABLE IF NOT EXISTS execution_results (
    id BIGSERIAL PRIMARY KEY,
    plan_id VARCHAR(255) NOT NULL UNIQUE,
    success BOOLEAN NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES plans(id)
);

-- Execution Log Entries table
CREATE TABLE IF NOT EXISTS execution_log_entries (
    id BIGSERIAL PRIMARY KEY,
    execution_result_id BIGINT NOT NULL,
    plan_id VARCHAR(255) NOT NULL,
    step_index INTEGER NOT NULL,
    step_type VARCHAR(50) NOT NULL,
    step_target VARCHAR(1000),
    step_explanation VARCHAR(2000),
    success BOOLEAN NOT NULL,
    message VARCHAR(2000),
    error VARCHAR(2000),
    executed_at TIMESTAMP NOT NULL,
    execution_time_ms BIGINT,
    screenshot_path VARCHAR(1000),
    logged_at TIMESTAMP NOT NULL,
    FOREIGN KEY (execution_result_id) REFERENCES execution_results(id) ON DELETE CASCADE
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_plans_entity_type_id ON plans(entity_type_id);
CREATE INDEX IF NOT EXISTS idx_plans_action_id ON plans(action_id);
CREATE INDEX IF NOT EXISTS idx_plans_status ON plans(status);
CREATE INDEX IF NOT EXISTS idx_plan_steps_plan_id ON plan_steps(plan_id);
CREATE INDEX IF NOT EXISTS idx_execution_results_plan_id ON execution_results(plan_id);
CREATE INDEX IF NOT EXISTS idx_execution_log_entries_plan_id ON execution_log_entries(plan_id);
CREATE INDEX IF NOT EXISTS idx_execution_log_entries_result_id ON execution_log_entries(execution_result_id);
