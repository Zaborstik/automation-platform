-- H2 resolves unquoted schema names as uppercase (e.g. system -> SYSTEM). Create schemas accordingly.
CREATE SCHEMA IF NOT EXISTS ZBRTSTK;
CREATE SCHEMA IF NOT EXISTS SYSTEM;
