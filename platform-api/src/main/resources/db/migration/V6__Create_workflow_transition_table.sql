CREATE TABLE IF NOT EXISTS system.workflow_transition (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow VARCHAR(36) NOT NULL,
    from_step VARCHAR(255) NOT NULL,
    to_step VARCHAR(255) NOT NULL,
    FOREIGN KEY (workflow) REFERENCES system.workflow(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_workflow_transition_uniq
    ON system.workflow_transition (workflow, from_step, to_step);

INSERT INTO system.workflow_transition (id, workflow, from_step, to_step) VALUES
    ('wft-1', 'wf-plan', 'new', 'in_progress'),
    ('wft-2', 'wf-plan', 'in_progress', 'paused'),
    ('wft-3', 'wf-plan', 'in_progress', 'completed'),
    ('wft-4', 'wf-plan', 'in_progress', 'failed'),
    ('wft-5', 'wf-plan', 'paused', 'in_progress'),
    ('wft-6', 'wf-plan', 'paused', 'cancelled'),
    ('wft-7', 'wf-plan', 'new', 'cancelled'),
    ('wft-8', 'wf-plan-step', 'new', 'in_progress'),
    ('wft-9', 'wf-plan-step', 'in_progress', 'completed'),
    ('wft-10', 'wf-plan-step', 'in_progress', 'failed'),
    ('wft-11', 'wf-plan-step', 'in_progress', 'paused'),
    ('wft-12', 'wf-plan-step', 'paused', 'in_progress'),
    ('wft-13', 'wf-plan-step', 'paused', 'cancelled'),
    ('wft-14', 'wf-plan-step', 'new', 'cancelled')
ON CONFLICT DO NOTHING;
