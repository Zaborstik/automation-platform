package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;

import java.util.List;

public interface StepExecutionCallback {

    void onStepStarted(PlanStep step, int stepIndex, int totalSteps);

    void onStepCompleted(PlanStep step, StepExecutionResult result, int stepIndex);

    void onPlanStarted(Plan plan);

    void onPlanCompleted(Plan plan, List<StepExecutionResult> results, boolean success);

    static StepExecutionCallback noOp() {
        return NoOpCallback.INSTANCE;
    }

    final class NoOpCallback implements StepExecutionCallback {
        private static final NoOpCallback INSTANCE = new NoOpCallback();

        private NoOpCallback() {
        }

        @Override
        public void onStepStarted(PlanStep step, int stepIndex, int totalSteps) {
        }

        @Override
        public void onStepCompleted(PlanStep step, StepExecutionResult result, int stepIndex) {
        }

        @Override
        public void onPlanStarted(Plan plan) {
        }

        @Override
        public void onPlanCompleted(Plan plan, List<StepExecutionResult> results, boolean success) {
        }
    }
}
