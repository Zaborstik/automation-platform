-- Plan step entity_id stores URLs/selectors and can exceed 36 chars.
-- Expand column length to avoid 22001 on scenario creation.
ALTER TABLE zbrtstk.plan_step
    ALTER COLUMN entity_id VARCHAR(510);
