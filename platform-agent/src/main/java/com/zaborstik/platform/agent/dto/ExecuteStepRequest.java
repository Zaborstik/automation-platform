package com.zaborstik.platform.agent.dto;

import com.zaborstik.platform.core.plan.PlanStep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Body of {@code POST /api/agent/execute-step}.
 *
 * <p>Carries everything the agent needs to perform one Playwright operation
 * without consulting any external Resolver:
 * <ul>
 *   <li>{@link #step} — the raw {@link PlanStep} from the plan</li>
 *   <li>{@link #operation} — the {@code system.action.internalname} value
 *       resolved upstream by {@code platform-executor}
 *       (e.g. {@code click}, {@code open_page})</li>
 *   <li>{@link #resolvedSelectors} — pre-fetched UI bindings
 *       ({@code actionId -> selector}) so {@code action(<id>)} targets resolve
 *       deterministically on the agent side</li>
 *   <li>{@link #stepIndex} — zero-based position of the step in the plan</li>
 * </ul>
 */
public class ExecuteStepRequest {

    @NotNull
    private PlanStep step;
    @NotBlank
    private String operation;
    private Map<String, String> resolvedSelectors;
    private int stepIndex;

    public ExecuteStepRequest() {
    }

    public PlanStep getStep() { return step; }
    public void setStep(PlanStep step) { this.step = step; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public Map<String, String> getResolvedSelectors() { return resolvedSelectors; }
    public void setResolvedSelectors(Map<String, String> resolvedSelectors) { this.resolvedSelectors = resolvedSelectors; }
    public int getStepIndex() { return stepIndex; }
    public void setStepIndex(int stepIndex) { this.stepIndex = stepIndex; }
}
