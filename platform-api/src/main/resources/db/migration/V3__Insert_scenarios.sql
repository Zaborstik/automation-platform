-- ========== СЦЕНАРИИ (ШАБЛОНЫ ПЛАНОВ) ==========
-- Идемпотентные вставки (WHERE NOT EXISTS) для совместимости с H2 и PostgreSQL.
--
-- У scenario_step с workflow = wf-plan-step колонка workflow_step_internalname — только ЖЦ шага (здесь new).
-- Тип UI-операции задаётся строкой action в scenario_step_action → system.action.internalname.

-- Сценарий 1: Поиск в DuckDuckGo и переход по первой ссылке (полный демо-сценарий)
INSERT INTO zbrtstk.scenario (
    id, name, target, explanation, workflow, workflow_step_internalname, created_time, updated_time
)
SELECT 'scen-duck-search-full', 'Поиск в DuckDuckGo и переход по первому результату', 'Найти информацию в интернете',
    'Открыть DuckDuckGo, ввести запрос, дождаться результатов и открыть первую ссылку.', 'wf-plan', 'new', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario WHERE id = 'scen-duck-search-full');

INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-full-1', 'scen-duck-search-full', 'wf-plan-step', 'new', 'ent-page', 'https://duckduckgo.com', 0, 'Открыть DuckDuckGo', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-full-1');
INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-full-2', 'scen-duck-search-full', 'wf-plan-step', 'new', 'ent-input', 'input[name=''q'']', 1, 'Ввести запрос и нажать Enter', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-full-2');
INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-full-3', 'scen-duck-search-full', 'wf-plan-step', 'new', 'ent-page', 'article[data-testid=''result'']', 2, 'Дождаться результатов поиска', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-full-3');
INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-full-4', 'scen-duck-search-full', 'wf-plan-step', 'new', 'ent-link', 'article[data-testid=''result''] h2 a', 3, 'Открыть первый результат', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-full-4');
INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-full-5', 'scen-duck-search-full', 'wf-plan-step', 'new', 'ent-page', 'domcontentloaded', 4, 'Дождаться загрузки страницы', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-full-5');

INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-full-1', 'act-open-page', 'https://duckduckgo.com'
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-full-1' AND action = 'act-open-page');
INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-full-2', 'act-input-text', 'Spring Boot Playwright browser automation demo\n'
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-full-2' AND action = 'act-input-text');
INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-full-3', 'act-wait-element', NULL
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-full-3' AND action = 'act-wait-element');
INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-full-4', 'act-click', NULL
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-full-4' AND action = 'act-click');
INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-full-5', 'act-wait-element', NULL
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-full-5' AND action = 'act-wait-element');

-- Сценарий 2: Только открыть DuckDuckGo
INSERT INTO zbrtstk.scenario (id, name, target, explanation, workflow, workflow_step_internalname, created_time, updated_time)
SELECT 'scen-duck-open-only', 'Открыть DuckDuckGo', 'Открыть поисковую страницу', 'Открыть главную страницу DuckDuckGo без поиска.', 'wf-plan', 'new', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario WHERE id = 'scen-duck-open-only');

INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-open-1', 'scen-duck-open-only', 'wf-plan-step', 'new', 'ent-page', 'https://duckduckgo.com', 0, 'Открыть DuckDuckGo', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-open-1');

INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-open-1', 'act-open-page', 'https://duckduckgo.com'
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-open-1' AND action = 'act-open-page');

-- Сценарий 3: Поиск в DuckDuckGo без перехода по ссылке
INSERT INTO zbrtstk.scenario (id, name, target, explanation, workflow, workflow_step_internalname, created_time, updated_time)
SELECT 'scen-duck-search-only', 'Поиск в DuckDuckGo (без перехода по ссылке)', 'Найти результаты поиска', 'Открыть DuckDuckGo, ввести запрос и дождаться выдачи результатов.', 'wf-plan', 'new', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario WHERE id = 'scen-duck-search-only');

INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-nc-1', 'scen-duck-search-only', 'wf-plan-step', 'new', 'ent-page', 'https://duckduckgo.com', 0, 'Открыть DuckDuckGo', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-nc-1');
INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-nc-2', 'scen-duck-search-only', 'wf-plan-step', 'new', 'ent-input', 'input[name=''q'']', 1, 'Ввести запрос и нажать Enter', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-nc-2');
INSERT INTO zbrtstk.scenario_step (id, scenario, workflow, workflow_step_internalname, entitytype, entity_id, sortorder, displayname, created_time, updated_time)
SELECT 'sst-duck-nc-3', 'scen-duck-search-only', 'wf-plan-step', 'new', 'ent-page', 'article[data-testid=''result'']', 2, 'Дождаться результатов поиска', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step WHERE id = 'sst-duck-nc-3');

INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-nc-1', 'act-open-page', 'https://duckduckgo.com'
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-nc-1' AND action = 'act-open-page');
INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-nc-2', 'act-input-text', 'Spring Boot Playwright browser automation demo\n'
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-nc-2' AND action = 'act-input-text');
INSERT INTO zbrtstk.scenario_step_action (scenario_step, action, meta_value)
SELECT 'sst-duck-nc-3', 'act-wait-element', NULL
WHERE NOT EXISTS (SELECT 1 FROM zbrtstk.scenario_step_action WHERE scenario_step = 'sst-duck-nc-3' AND action = 'act-wait-element');
