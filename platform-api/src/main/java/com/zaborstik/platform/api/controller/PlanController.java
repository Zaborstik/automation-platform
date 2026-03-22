package com.zaborstik.platform.api.controller;

import com.zaborstik.platform.api.dto.*;
import com.zaborstik.platform.api.entity.PlanResultEntity;
import com.zaborstik.platform.api.entity.PlanStepLogEntryEntity;
import com.zaborstik.platform.api.service.PlanExecutionService;
import com.zaborstik.platform.api.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * REST контроллер планов по новой логике: создание плана (после LLM/RAD), получение, результат и лог шага.
 */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "Plans")
public class PlanController {

    private final PlanService planService;
    private final PlanExecutionService planExecutionService;

    public PlanController(PlanService planService, PlanExecutionService planExecutionService) {
        this.planService = planService;
        this.planExecutionService = planExecutionService;
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
    @Operation(summary = "Create plan")
    @ApiResponse(responseCode = "201", description = "Plan created")
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        PlanResponse created = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get plan by id")
    @ApiResponse(responseCode = "200", description = "Plan found")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    public ResponseEntity<?> getPlan(@PathVariable("id") String id) {
        return planService.getPlan(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    ErrorResponseDTO error = new ErrorResponseDTO(
                            HttpStatus.NOT_FOUND.value(),
                            "Not Found",
                            "Plan with id '" + id + "' not found",
                            "/api/plans/" + id
                    );
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                });
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
    public ResponseEntity<PlanStepLogEntryEntity> createPlanStepLogEntry(
            @PathVariable("planId") String planId,
            @Valid @RequestBody CreatePlanStepLogEntryRequest request) {
        PlanStepLogEntryEntity entry = planService.createPlanStepLogEntry(
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

    @PostMapping("/{planId}/execute")
    @Operation(summary = "Execute plan")
    @ApiResponse(responseCode = "200", description = "Plan executed")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    public ResponseEntity<?> executePlan(@PathVariable("planId") String planId) {
        return planExecutionService.executePlan(planId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> {
                ErrorResponseDTO error = new ErrorResponseDTO(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    "Plan with id '" + planId + "' not found",
                    "/api/plans/" + planId + "/execute"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            });
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
            ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "Plan with id '" + id + "' not found",
                "/api/plans/" + id + "/transition"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
}
