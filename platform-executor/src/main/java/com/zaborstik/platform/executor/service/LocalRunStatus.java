package com.zaborstik.platform.executor.service;

/**
 * Coarse status of a local plan run tracked by {@link LocalRunService}.
 */
public enum LocalRunStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
}
