-- Добавить переходы для типов шагов (open_page, wait, type, click и др.) в in_progress.
-- Executor вызывает transitionPlanStep(stepId, "in_progress") при текущем workflow_step = типу шага.
INSERT INTO system.workflow_transition (id, workflow, from_step, to_step) VALUES
    ('wft-20', 'wf-plan-step', 'open_page', 'in_progress'),
    ('wft-21', 'wf-plan-step', 'wait', 'in_progress'),
    ('wft-22', 'wf-plan-step', 'type', 'in_progress'),
    ('wft-23', 'wf-plan-step', 'click', 'in_progress'),
    ('wft-24', 'wf-plan-step', 'select_option', 'in_progress'),
    ('wft-25', 'wf-plan-step', 'read_text', 'in_progress'),
    ('wft-26', 'wf-plan-step', 'take_screenshot', 'in_progress'),
    ('wft-27', 'wf-plan-step', 'explain', 'in_progress'),
    ('wft-28', 'wf-plan-step', 'hover', 'in_progress');
