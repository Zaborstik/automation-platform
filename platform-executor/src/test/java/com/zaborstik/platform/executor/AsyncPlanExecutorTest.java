package com.zaborstik.platform.executor;

import com.zaborstik.platform.core.plan.Plan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncPlanExecutorTest {

    @Mock
    private PlanExecutor planExecutor;

    @Test
    void executeAsyncShouldReturnFutureWithResult() throws Exception {
        Plan plan = emptyPlan("p1");
        PlanExecutionResult expected = new PlanExecutionResult("p1", true, Instant.now(), Instant.now(), List.of());
        when(planExecutor.execute(any(Plan.class), anyBoolean())).thenReturn(expected);

        AsyncPlanExecutor async = new AsyncPlanExecutor(planExecutor);
        CompletableFuture<PlanExecutionResult> future = async.executeAsync(plan);

        PlanExecutionResult actual = future.get(2, TimeUnit.SECONDS);
        assertEquals("p1", actual.planId());
        async.shutdown();
    }

    @Test
    void multiplePlansShouldRunInParallel() throws Exception {
        when(planExecutor.execute(any(Plan.class), anyBoolean())).thenAnswer(invocation -> {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(200));
            Plan p = invocation.getArgument(0);
            return new PlanExecutionResult(p.id(), true, Instant.now(), Instant.now(), List.of());
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        AsyncPlanExecutor async = new AsyncPlanExecutor(planExecutor, pool);

        long started = System.currentTimeMillis();
        CompletableFuture<PlanExecutionResult> f1 = async.executeAsync(emptyPlan("p1"));
        CompletableFuture<PlanExecutionResult> f2 = async.executeAsync(emptyPlan("p2"));
        CompletableFuture.allOf(f1, f2).get(2, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - started;

        assertTrue(elapsed < 350, "Expected parallel execution, elapsed=" + elapsed);
        async.shutdown();
    }

    @Test
    void shutdownShouldRejectNewTasks() {
        AsyncPlanExecutor async = new AsyncPlanExecutor(planExecutor, Executors.newSingleThreadExecutor());
        async.shutdown();

        assertThrows(RejectedExecutionException.class, () -> async.executeAsync(emptyPlan("p1")));
    }

    private Plan emptyPlan(String id) {
        return new Plan(id, "wf-plan", "new", "none", null, null, List.of());
    }
}
