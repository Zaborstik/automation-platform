package com.zaborstik.platform.executor.service;

import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.executor.PlanExecutionResult;
import com.zaborstik.platform.executor.PlanExecutor;
import com.zaborstik.platform.executor.client.RemoteApiClient;
import com.zaborstik.platform.executor.client.api.PlanDto;
import com.zaborstik.platform.executor.client.api.PlanDtoMapper;
import com.zaborstik.platform.executor.resolver.RemoteResolver;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * High-level orchestrator on the local side. Owns the small in-memory map of
 * {@code runId -> LocalRunRecord} surfaced by the {@code /local/status}
 * endpoint and dispatches plan execution to {@link PlanExecutor} on a
 * background thread so that REST callers do not block.
 */
@Service
public class LocalRunService {

    private static final Logger log = LoggerFactory.getLogger(LocalRunService.class);

    private final RemoteApiClient apiClient;
    private final PlanExecutor planExecutor;
    private final RemoteResolver remoteResolver;
    private final ExecutorService backgroundPool;
    private final String defaultBrowserBaseUrl;
    private final boolean defaultHeadless;

    private final ConcurrentHashMap<String, LocalRunRecord> runs = new ConcurrentHashMap<>();

    public LocalRunService(RemoteApiClient apiClient,
                           PlanExecutor planExecutor,
                           RemoteResolver remoteResolver,
                           @Value("${platform.agent.base-url:http://localhost:8080}") String defaultBrowserBaseUrl,
                           @Value("${platform.agent.headless:false}") boolean defaultHeadless) {
        this.apiClient = apiClient;
        this.planExecutor = planExecutor;
        this.remoteResolver = remoteResolver;
        this.defaultBrowserBaseUrl = defaultBrowserBaseUrl;
        this.defaultHeadless = defaultHeadless;
        this.backgroundPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "local-run-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public LocalRunRecord runFromUserInput(String userInput, String browserBaseUrl, Boolean headless) {
        PlanDto generated = apiClient.createPlanFromRequest(userInput);
        return runPlan(PlanDtoMapper.toDomain(generated), browserBaseUrl, headless);
    }

    public LocalRunRecord runExistingPlan(String planId, String browserBaseUrl, Boolean headless) {
        PlanDto dto = apiClient.fetchPlan(planId)
            .orElseThrow(() -> new NoSuchElementException("Plan not found on server: " + planId));
        return runPlan(PlanDtoMapper.toDomain(dto), browserBaseUrl, headless);
    }

    public Optional<LocalRunRecord> getRun(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    private LocalRunRecord runPlan(Plan plan, String browserBaseUrl, Boolean headless) {
        LocalRunRecord record = new LocalRunRecord(UUID.randomUUID().toString());
        record.setPlanId(plan.id());
        runs.put(record.getRunId(), record);

        String baseUrl = browserBaseUrl != null && !browserBaseUrl.isBlank()
            ? browserBaseUrl : defaultBrowserBaseUrl;
        boolean useHeadless = headless != null ? headless : defaultHeadless;

        backgroundPool.submit(() -> safelyExecute(record, plan, baseUrl, useHeadless));
        return record;
    }

    private void safelyExecute(LocalRunRecord record, Plan plan, String baseUrl, boolean headless) {
        record.setStatus(LocalRunStatus.RUNNING);
        try {
            remoteResolver.resetCache();
            PlanExecutionResult result = planExecutor.execute(plan, baseUrl, headless);
            record.setPlanResultId(record.getPlanResultId());
            record.setTotalSteps(result.logEntries().size());
            record.setFailedSteps((int) result.logEntries().stream()
                .filter(e -> !e.result().success()).count());
            record.setStatus(result.success() ? LocalRunStatus.SUCCEEDED : LocalRunStatus.FAILED);
            record.setMessage(result.success()
                ? "Plan completed successfully"
                : "Plan finished with " + record.getFailedSteps() + " failed step(s)");
        } catch (Exception ex) {
            log.error("Local run {} for plan {} failed", record.getRunId(), plan.id(), ex);
            record.setStatus(LocalRunStatus.FAILED);
            record.setMessage("Local run failed: " + ex.getMessage());
        } finally {
            record.setFinishedAt(java.time.Instant.now());
        }
    }

    @PreDestroy
    public void shutdown() {
        backgroundPool.shutdown();
        try {
            if (!backgroundPool.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundPool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            backgroundPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
