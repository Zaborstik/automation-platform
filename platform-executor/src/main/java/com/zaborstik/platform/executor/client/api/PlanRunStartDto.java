package com.zaborstik.platform.executor.client.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Local mirror of {@code com.zaborstik.platform.api.dto.PlanRunResponse}
 * returned by {@code POST /api/plans/{id}/runs} and {@code /runs/finish}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanRunStartDto {

    public String planId;
    public String planResultId;
    public boolean success;
    public int totalSteps;
    public int failedSteps;
    public Instant startedTime;
    public Instant finishedTime;
}
