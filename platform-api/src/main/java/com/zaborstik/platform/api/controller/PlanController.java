package com.zaborstik.platform.api.controller;

import com.zaborstik.platform.api.client.KnowledgeClient;
import com.zaborstik.platform.api.dto.CreatePlanFromRequestRequest;
import com.zaborstik.platform.api.dto.CreatePlanRequest;
import com.zaborstik.platform.api.dto.CreatePlanResultRequest;
import com.zaborstik.platform.api.dto.CreatePlanStepLogRequest;
import com.zaborstik.platform.api.dto.ErrorResponseDTO;
import com.zaborstik.platform.api.dto.FinishPlanRunRequest;
import com.zaborstik.platform.api.dto.PlanResponse;
import com.zaborstik.platform.api.dto.PlanRunResponse;
import com.zaborstik.platform.api.dto.StepExecutionReportRequest;
import com.zaborstik.platform.api.dto.TransitionPlanRequest;
import com.zaborstik.platform.api.entity.PlanResultEntity;
import com.zaborstik.platform.api.entity.PlanStepLogEntity;
import com.zaborstik.platform.api.service.PlanExecutionService;
import com.zaborstik.platform.api.service.PlanService;
import com.zaborstik.platform.core.plan.Plan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * REST controller for plan resources used by both the chat UI (creating new
 * plans) and the local executor (fetching plan content, reporting per-step
 * results, closing runs).
 *
 * <p>Plan execution itself is intentionally not done here — see {@code
 * platform-executor} on the user's machine for that.
 */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "Plans")
public class PlanController {

    private final PlanService planService;
    private final PlanExecutionService planExecutionService;
    private final KnowledgeClient knowledgeClient;

    public PlanController(PlanService planService,
                          PlanExecutionService planExecutionService,
                          KnowledgeClient knowledgeClient) {
        this.planService = planService;
        this.planExecutionService = planExecutionService;
        this.knowledgeClient = knowledgeClient;
    }

    @GetMapping
    @Operation(summary = "List plans with optional status filter and pagination")
    @ApiResponse(responseCode = "200", description = "Plans page returned")
    public Page<PlanResponse> listPlans(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return planService.listPlans(status, PageRequest.of(page, size));
    }

    @PostMapping
    @Operation(summary = "Create plan from an explicit request body")
    @ApiResponse(responseCode = "201", description = "Plan created")
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        PlanResponse created = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/from-request")
    @Operation(summary = "Create a plan by delegating plan generation to platform-knowledge")
    @ApiResponse(responseCode = "201", description = "Plan generated and persisted")
    public ResponseEntity<PlanResponse> createPlanFromRequest(@Valid @RequestBody CreatePlanFromRequestRequest request) {
        Plan generated = knowledgeClient.generatePlan(request.getUserInput());
        PlanResponse persisted = planService.createPlanFromDomain(generated);
        return ResponseEntity.status(HttpStatus.CREATED).body(persisted);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get plan by id")
    @ApiResponse(responseCode = "200", description = "Plan found")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    public ResponseEntity<?> getPlan(@PathVariable("id") String id) {
        return planService.getPlan(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> notFound("Plan with id '" + id + "' not found", "/api/plans/" + id));
    }

    @PostMapping("/{planId}/result")
    @Operation(summary = "Create plan result")
    @ApiResponse(responseCode = "201", description = "Plan result created")
    public ResponseEntity<PlanResultEntity> createPlanResult(
            @PathVariable("planId") String planId,
            @Valid @RequestBody CreatePlanResultRequest request) {
        PlanResultEntity result = planService.createPlanResult(
                planId,
                request.getSuccess(),
                request.getStartedTime(),
                request.getFinishedTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{planId}/step-log")
    @Operation(summary = "Create plan step log entry")
    @ApiResponse(responseCode = "201", description = "Plan step log entry created")
    public ResponseEntity<PlanStepLogEntity> createPlanStepLog(
            @PathVariable("planId") String planId,
            @Valid @RequestBody CreatePlanStepLogRequest request) {
        PlanStepLogEntity entry = planService.createPlanStepLog(
                planId,
                request.getPlanStepId(),
                request.getPlanResultId(),
                request.getActionId(),
                request.getMessage(),
                request.getError(),
                request.getExecutedTime(),
                request.getExecutionTimeMs(),
                request.getAttachmentId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    /**
     * Called by {@code platform-executor} (local) right before it starts
     * processing plan steps. Server transitions the plan to {@code in_progress}
     * and returns the {@code planResultId} the executor must echo back in
     * every step report and in the closing call.
     */
    @PostMapping("/{planId}/runs")
    @Operation(summary = "Start an execution run for the plan (called by local executor)")
    @ApiResponse(responseCode = "200", description = "Run started")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    public ResponseEntity<?> startRun(@PathVariable("planId") String planId) {
        return planExecutionService.startRun(planId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> notFound("Plan with id '" + planId + "' not found",
                "/api/plans/" + planId + "/runs"));
    }

    /**
     * Called by {@code platform-executor} after each step of a plan run.
     */
    @PostMapping("/{planId}/steps/{stepId}/result")
    @Operation(summary = "Report the outcome of a plan step from the local executor")
    @ApiResponse(responseCode = "204", description = "Step result accepted")
    public ResponseEntity<Void> reportStepResult(
        @PathVariable("planId") String planId,
        @PathVariable("stepId") String stepId,
        @RequestParam("planResultId") String planResultId,
        @Valid @RequestBody StepExecutionReportRequest request) {
        planExecutionService.reportStepResult(planId, stepId, planResultId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planId}/runs/finish")
    @Operation(summary = "Finalise a plan run (called by local executor on completion)")
    @ApiResponse(responseCode = "200", description = "Run finalised")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    public ResponseEntity<?> finishRun(@PathVariable("planId") String planId,
                                        @Valid @RequestBody FinishPlanRunRequest request) {
        return planExecutionService.finishRun(
                planId,
                request.getPlanResultId(),
                Boolean.TRUE.equals(request.getSuccess()),
                request.getTotalSteps(),
                request.getFailedSteps(),
                request.getStartedTime(),
                request.getFinishedTime()
            )
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> notFound("Plan with id '" + planId + "' not found",
                "/api/plans/" + planId + "/runs/finish"));
    }

    @PatchMapping("/{id}/transition")
    @Operation(summary = "Transition plan lifecycle status")
    @ApiResponse(responseCode = "200", description = "Plan transitioned")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @ApiResponse(responseCode = "409", description = "Invalid transition")
    public ResponseEntity<?> transitionPlan(@PathVariable("id") String id, @Valid @RequestBody TransitionPlanRequest request) {
        try {
            PlanResponse response = planService.transitionPlan(id, request.getTargetStep());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException ex) {
            return notFound("Plan with id '" + id + "' not found", "/api/plans/" + id + "/transition");
        } catch (IllegalStateException ex) {
            ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                "/api/plans/" + id + "/transition"
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    private static ResponseEntity<ErrorResponseDTO> notFound(String message, String path) {
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            message,
            path
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
