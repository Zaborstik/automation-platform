-- Расширение колонки error для длинных сообщений об ошибках Playwright
ALTER TABLE zbrtstk.plan_step_log_entry ALTER COLUMN error VARCHAR(2000);
