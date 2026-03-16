package com.zaborstik.platform.executor;

import com.zaborstik.platform.core.plan.Plan;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncPlanExecutor {
    private final PlanExecutor planExecutor;
    private final ExecutorService executorService;

    public AsyncPlanExecutor(PlanExecutor planExecutor) {
        this(planExecutor, Executors.newCachedThreadPool());
    }

    public AsyncPlanExecutor(PlanExecutor planExecutor, ExecutorService executorService) {
        this.planExecutor = Objects.requireNonNull(planExecutor, "planExecutor cannot be null");
        this.executorService = Objects.requireNonNull(executorService, "executorService cannot be null");
    }

    public CompletableFuture<PlanExecutionResult> executeAsync(Plan plan) {
        return executeAsync(plan, false);
    }

    public CompletableFuture<PlanExecutionResult> executeAsync(Plan plan, boolean stopOnFailure) {
        return CompletableFuture.supplyAsync(() -> planExecutor.execute(plan, stopOnFailure), executorService);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
