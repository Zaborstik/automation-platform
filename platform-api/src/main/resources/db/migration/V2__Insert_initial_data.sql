-- ========== INITIAL DICTIONARIES ==========

-- workflow_step: универсальные шаги жизненного цикла
INSERT INTO system.workflow_step (id, internalname, displayname, sortorder)
VALUES
    ('wfs-new', 'new', 'Новая', 10),
    ('wfs-in-progress', 'in_progress', 'В работе', 20),
    ('wfs-paused', 'paused', 'Приостановлена', 30),
    ('wfs-completed', 'completed', 'Завершена', 40),
    ('wfs-failed', 'failed', 'Ошибка', 50),
    ('wfs-cancelled', 'cancelled', 'Отменена', 60)
ON CONFLICT DO NOTHING;

-- workflow: ЖЦ для plan и plan_step
INSERT INTO system.workflow (id, displayname, firststep)
VALUES
    ('wf-plan', 'Жизненный цикл плана', 'wfs-new'),
    ('wf-plan-step', 'Жизненный цикл шага плана', 'wfs-new')
ON CONFLICT DO NOTHING;

-- action_type: типы действий в RAD/LLM-среде
INSERT INTO system.action_type (id, internalname, displayname)
VALUES
    ('act-type-navigation', 'navigation', 'Навигация'),
    ('act-type-interaction', 'interaction', 'Взаимодействие с UI'),
    ('act-type-data-input', 'data_input', 'Ввод данных'),
    ('act-type-validation', 'validation', 'Проверка результата'),
    ('act-type-artifact', 'artifact', 'Артефакты выполнения')
ON CONFLICT DO NOTHING;

-- action: базовые действия платформы
INSERT INTO system.action (
    id,
    displayname,
    internalname,
    meta_value,
    description,
    action_type,
    created_time,
    updated_time
)
VALUES
    (
        'act-open-page',
        'Открыть страницу',
        'open_page',
        NULL,
        'Переход на страницу по URL.',
        'act-type-navigation',
        NOW(),
        NOW()
    ),
    (
        'act-click',
        'Клик по элементу',
        'click',
        NULL,
        'Нажатие на элемент интерфейса.',
        'act-type-interaction',
        NOW(),
        NOW()
    ),
    (
        'act-input-text',
        'Ввести текст',
        'input_text',
        NULL,
        'Ввод текста в поле/контрол.',
        'act-type-data-input',
        NOW(),
        NOW()
    ),
    (
        'act-select-option',
        'Выбрать опцию',
        'select_option',
        NULL,
        'Выбор значения в списке/селекте.',
        'act-type-data-input',
        NOW(),
        NOW()
    ),
    (
        'act-wait-element',
        'Ожидать элемент',
        'wait_element',
        NULL,
        'Ожидание появления/доступности элемента.',
        'act-type-validation',
        NOW(),
        NOW()
    ),
    (
        'act-read-text',
        'Считать текст',
        'read_text',
        NULL,
        'Чтение текста элемента для анализа/проверки.',
        'act-type-validation',
        NOW(),
        NOW()
    ),
    (
        'act-take-screenshot',
        'Сделать скриншот',
        'take_screenshot',
        NULL,
        'Сохранение скриншота как артефакта выполнения.',
        'act-type-artifact',
        NOW(),
        NOW()
    )
ON CONFLICT DO NOTHING;

-- entity_type и применимость действий (таблица system.entity_type создана в V1)
INSERT INTO system.entity_type (
    id,
    displayname,
    created_time,
    updated_time,
    km_article,
    ui_description,
    entityfieldlist,
    buttons
)
VALUES
    ('ent-page', 'Страница', NOW(), NOW(), NULL, 'Контейнер экрана/вкладки.', NULL, NULL),
    ('ent-form', 'Форма', NOW(), NOW(), NULL, 'Форма ввода данных.', NULL, NULL),
    ('ent-input', 'Поле ввода', NOW(), NOW(), NULL, 'Текстовое поле/контрол ввода.', NULL, NULL),
    ('ent-button', 'Кнопка', NOW(), NOW(), NULL, 'Кнопка действия.', NULL, NULL),
    ('ent-link', 'Ссылка', NOW(), NOW(), NULL, 'Навигационная ссылка.', NULL, NULL),
    ('ent-table', 'Таблица', NOW(), NOW(), NULL, 'Табличные данные.', NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO system.action_applicable_entity_type (action, entity_type)
VALUES
    ('act-open-page', 'ent-page'),
    ('act-click', 'ent-button'),
    ('act-click', 'ent-link'),
    ('act-input-text', 'ent-input'),
    ('act-select-option', 'ent-input'),
    ('act-wait-element', 'ent-page'),
    ('act-wait-element', 'ent-form'),
    ('act-read-text', 'ent-table'),
    ('act-read-text', 'ent-page'),
    ('act-take-screenshot', 'ent-page')
ON CONFLICT DO NOTHING;
