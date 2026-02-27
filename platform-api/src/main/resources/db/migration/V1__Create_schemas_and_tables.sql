-- Схема БД по docs/newdatabase.drawio
-- Бизнес-логика: схема zbrtstk. Всё для работы приложения: схема system.
-- VJ = VARCHAR(10000).

-- ========== СХЕМЫ ==========
CREATE SCHEMA IF NOT EXISTS zbrtstk;
CREATE SCHEMA IF NOT EXISTS system;

-- -- ========== ZBRTSTK (бизнес-логика) ==========
-- -- модель строит action_applicable_entity_type на основе этих данных, мб нужна будет векторка
-- -- entity_type: Типы сущностей
-- CREATE TABLE IF NOT EXISTS zbrtstk.entity_type (
--     id VARCHAR(36) NOT NULL PRIMARY KEY,
--     displayname VARCHAR(255) NOT NULL,
--     created_time TIMESTAMP NOT NULL,
--     updated_time TIMESTAMP NOT NULL,
--     km_article VARCHAR(36),
--     ui_description TEXT,
--     entityfieldlist VARCHAR(10000),
--     buttons VARCHAR(10000)
-- );
--
-- -- entity_type_metadata: Метаданные типа (ключ-значение)
-- CREATE TABLE IF NOT EXISTS zbrtstk.entity_type_metadata (
--     id VARCHAR(36) NOT NULL PRIMARY KEY,
--     entity_type VARCHAR(36) NOT NULL,
--     meta_key VARCHAR(36) NOT NULL,
--     meta_value TEXT,
--     FOREIGN KEY (entity_type) REFERENCES zbrtstk.entity_type(id) ON DELETE CASCADE
-- );

-- ========== SYSTEM (платформа) ==========

-- workflow_step: Шаг ЖЦ
CREATE TABLE IF NOT EXISTS system.workflow_step (
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    internalname VARCHAR(255) NOT NULL, -- имя на английском, типа new in_progres
    displayname VARCHAR(255) NOT NULL, -- отображаемое пользователю имя
    sortorder INT -- номер по счёту -- индекс
);

-- workflow: ЖЦ
CREATE TABLE IF NOT EXISTS system.workflow (
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    displayname VARCHAR(255) NOT NULL, -- отображаемое пользователю имя -- индекс
    firststep VARCHAR(36) NOT NULL, -- первый шаг
    FOREIGN KEY (firststep) REFERENCES system.workflow_step(id)
);

-- ui_binding: Привязка действия к UI (селектор)
CREATE TABLE IF NOT EXISTS system.ui_binding ( -- не финальные, тут хз
    action VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    selector VARCHAR(510) NOT NULL,
    selector_type VARCHAR(36) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    FOREIGN KEY (action) REFERENCES system.action(id) ON DELETE CASCADE
);

-- ui_binding_metadata: Метаданные привязки к UI
CREATE TABLE IF NOT EXISTS system.ui_binding_metadata ( -- не финальные, тут хз
    ui_binding VARCHAR(36) NOT NULL,
    meta_key VARCHAR(36) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (ui_binding, meta_key), -- юник индекс
    FOREIGN KEY (ui_binding) REFERENCES system.ui_binding(action) ON DELETE CASCADE
);

-- attachment: Вложение
CREATE TABLE IF NOT EXISTS system.attachment (
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    displayname VARCHAR(255) -- отображаемое пользователю имя -- индекс
);

-- plan: План выполнения
CREATE TABLE IF NOT EXISTS system.plan (
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    workflow VARCHAR(36) NOT NULL,
    workflow_step_internalname VARCHAR(255) NOT NULL, -- шаг ЖЦ
    stopped_at_plan_step VARCHAR(36) NOT NULL, -- остановился на шаге плана
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP,
    target VARCHAR(510), -- цель, пока хз нужно ли это поле
    explanation VARCHAR(1020), -- инфа для пользователя
    FOREIGN KEY (workflow) REFERENCES system.workflow(id)
);

-- plan_step: Шаги плана
CREATE TABLE IF NOT EXISTS system.plan_step (
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    plan VARCHAR(36) NOT NULL, -- индекс
    workflow VARCHAR(36) NOT NULL,
    workflow_step_internalname VARCHAR(255) NOT NULL, -- шаг ЖЦ
    entitytype VARCHAR(36) NOT NULL,
    entity_id VARCHAR(36), -- объект дочерней системы, в которой происходят действия
    sortorder INT NOT NULL, -- порядковый номер шага -- индекс
    displayname VARCHAR(255) NOT NULL, -- отображаемое пользователю имя
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    FOREIGN KEY (plan) REFERENCES system.plan(id) ON DELETE CASCADE,
    FOREIGN KEY (workflow) REFERENCES system.workflow(id),
    FOREIGN KEY (entitytype) REFERENCES zbrtstk.entity_type(id)
);

-- plan_step_parameter: Параметры шага (ключ-значение)
CREATE TABLE IF NOT EXISTS system.plan_step_parameter ( -- пока хз надо ли
    plan_step VARCHAR(36) NOT NULL,
    meta_key VARCHAR(36) NOT NULL,
    meta_value TEXT, -- хранит инфу типа, как назвать объект и тд 
    PRIMARY KEY (plan_step, meta_key), -- юник индекс
    FOREIGN KEY (plan_step) REFERENCES system.plan_step(id) ON DELETE CASCADE
);

-- plan_step_action: связка шаг - действия
CREATE TABLE IF NOT EXISTS system.plan_step_action (
    plan_step VARCHAR(36) NOT NULL, -- индекс
    action VARCHAR(36) NOT NULL,
    PRIMARY KEY (plan_step, action), -- юник индекс
    FOREIGN KEY (plan_step) REFERENCES system.plan_step(id) ON DELETE CASCADE,
    FOREIGN KEY (action) REFERENCES system.action(id) ON DELETE CASCADE
);

-- action_type: Типы действия
CREATE TABLE IF NOT EXISTS system.action_type (
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    internalname VARCHAR(255) NOT NULL, -- имя на английском, типа open clic
    displayname VARCHAR(255) NOT NULL -- отображаемое пользователю имя
);

-- action: Действия платформы
CREATE TABLE IF NOT EXISTS system.action (
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    displayname VARCHAR(255) NOT NULL, -- отображаемое пользователю имя
    internalname VARCHAR(255) NOT NULL, -- имя на английском, типа open clic -- индекс
    description VARCHAR(255), -- описание для пользователя
    action_type VARCHAR(36) NOT NULL, -- индекс
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL,
    FOREIGN KEY (action_type) REFERENCES system.action_type(id)
);

-- action_applicable_entity_type: Действия, применимые к объекту дочерней системы
CREATE TABLE IF NOT EXISTS system.action_applicable_entity_type (
    action VARCHAR(36) NOT NULL,
    entity_type VARCHAR(36) NOT NULL, -- тип объекта
    PRIMARY KEY (action, entity_type), -- юник индекс
    FOREIGN KEY (action) REFERENCES system.action(id) ON DELETE CASCADE,
    FOREIGN KEY (entity_type) REFERENCES zbrtstk.entity_type(id) ON DELETE CASCADE
);

-- action_metadata: Метаданные действия
CREATE TABLE IF NOT EXISTS system.action_metadata (
    action VARCHAR(36) NOT NULL,
    meta_key VARCHAR(36) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (action, meta_key), -- юник индекс
    FOREIGN KEY (action) REFERENCES system.action(id) ON DELETE CASCADE
);

-- execution_result: Итог выполнения плана
CREATE TABLE IF NOT EXISTS system.plan_result (
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    plan VARCHAR(36) NOT NULL, -- индекс ??
    success BOOLEAN NOT NULL,
    started_time TIMESTAMP NOT NULL,
    finished_time TIMESTAMP NOT NULL,
    FOREIGN KEY (plan) REFERENCES system.plan(id)
);

-- execution_log_entry: Лог по шагам
CREATE TABLE IF NOT EXISTS system.plan_step_log_entry ( -- создаётся в случае падения
    id VARCHAR(36) NOT NULL PRIMARY KEY, -- юник индекс
    plan VARCHAR(36) NOT NULL, -- индекс
    plan_step VARCHAR(36) NOT NULL,
    plan_result VARCHAR(36) NOT NULL,
    action VARCHAR(36) NOT NULL,
    message VARCHAR(510) NOT NULL, -- сообщение которое написал пользователь
    error VARCHAR(510), -- если вдруг выполнение упало
    executed_time TIMESTAMP NOT NULL,
    execution_time_ms BIGINT,
    attachment VARCHAR(36), -- вложение, мб скрин, либо что-то такое, а мб в будущем сделать несколько вложений
    FOREIGN KEY (plan) REFERENCES system.plan(id),
    FOREIGN KEY (plan_step) REFERENCES system.plan_step(id),
    FOREIGN KEY (plan_result) REFERENCES system.plan_result(id) ON DELETE CASCADE,
    FOREIGN KEY (action) REFERENCES system.action(id),
    FOREIGN KEY (attachment) REFERENCES system.attachment(id)
);


-- -- Индексы
-- CREATE INDEX IF NOT EXISTS idx_plan_action ON system.plan(action);
-- CREATE INDEX IF NOT EXISTS idx_plan_workflow ON system.plan(workflow);
-- CREATE INDEX IF NOT EXISTS idx_plan_entity_type_id ON system.plan(entity_type_id);
-- CREATE INDEX IF NOT EXISTS idx_plan_step_plan ON system.plan_step(plan);
-- CREATE INDEX IF NOT EXISTS idx_plan_step_step_type ON system.plan_step(step_type);
-- CREATE INDEX IF NOT EXISTS idx_execution_result_plan ON system.execution_result(plan);
-- CREATE INDEX IF NOT EXISTS idx_execution_log_result ON system.execution_log_entry(execution_result);
-- CREATE INDEX IF NOT EXISTS idx_execution_log_plan ON system.execution_log_entrie(plan);
