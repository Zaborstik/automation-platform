-- ========== ШАБЛОНЫ СЦЕНАРИЕВ (ПЛАНЫ) ==========
-- Сценарий — шаблон для создания плана. Хранит целевой URL/описание и шаги с действиями.

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
