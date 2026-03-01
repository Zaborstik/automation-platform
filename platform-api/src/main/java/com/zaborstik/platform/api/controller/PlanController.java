package com.zaborstik.platform.api.controller;

import com.zaborstik.platform.api.dto.*;
import com.zaborstik.platform.api.entity.PlanResultEntity;
import com.zaborstik.platform.api.entity.PlanStepLogEntryEntity;
import com.zaborstik.platform.api.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер планов по новой логике: создание плана (после LLM/RAD), получение, результат и лог шага.
 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        PlanResponse created = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
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
}
