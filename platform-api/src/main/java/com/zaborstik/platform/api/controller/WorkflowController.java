package com.zaborstik.platform.api.controller;

import com.zaborstik.platform.api.dto.ErrorResponseDTO;
import com.zaborstik.platform.api.dto.WorkflowResponse;
import com.zaborstik.platform.api.dto.WorkflowStepResponse;
import com.zaborstik.platform.api.entity.WorkflowEntity;
import com.zaborstik.platform.api.entity.WorkflowStepEntity;
import com.zaborstik.platform.api.repository.WorkflowRepository;
import com.zaborstik.platform.api.repository.WorkflowStepRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Workflows")
public class WorkflowController {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;

    public WorkflowController(WorkflowRepository workflowRepository, WorkflowStepRepository workflowStepRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
    }

    @GetMapping("/api/workflows")
    @Operation(summary = "List workflows")
    @ApiResponse(responseCode = "200", description = "Workflows returned")
    public ResponseEntity<List<WorkflowResponse>> listWorkflows() {
        List<WorkflowResponse> response = workflowRepository.findAll().stream()
            .map(this::toWorkflowResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/workflows/{id}")
    @Operation(summary = "Get workflow by id")
    @ApiResponse(responseCode = "200", description = "Workflow found")
    @ApiResponse(responseCode = "404", description = "Workflow not found")
    public ResponseEntity<?> getWorkflowById(@PathVariable("id") String id) {
        return workflowRepository.findById(id)
            .<ResponseEntity<?>>map(entity -> ResponseEntity.ok(toWorkflowResponse(entity)))
            .orElseGet(() -> workflowNotFound(id));
    }

    @GetMapping("/api/workflow-steps")
    @Operation(summary = "List workflow steps")
    @ApiResponse(responseCode = "200", description = "Workflow steps returned")
    public ResponseEntity<List<WorkflowStepResponse>> listWorkflowSteps() {
        List<WorkflowStepResponse> response = workflowStepRepository.findAll().stream()
            .map(this::toWorkflowStepResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/workflow-steps/{id}")
    @Operation(summary = "Get workflow step by id")
    @ApiResponse(responseCode = "200", description = "Workflow step found")
    @ApiResponse(responseCode = "404", description = "Workflow step not found")
    public ResponseEntity<?> getWorkflowStepById(@PathVariable("id") String id) {
        return workflowStepRepository.findById(id)
            .<ResponseEntity<?>>map(entity -> ResponseEntity.ok(toWorkflowStepResponse(entity)))
            .orElseGet(() -> workflowStepNotFound(id));
    }

    private WorkflowResponse toWorkflowResponse(WorkflowEntity entity) {
        WorkflowResponse response = new WorkflowResponse();
        response.setId(entity.getId());
        response.setDisplayname(entity.getDisplayname());
        response.setFirstStepId(entity.getFirststep() != null ? entity.getFirststep().getId() : null);
        return response;
    }

    private WorkflowStepResponse toWorkflowStepResponse(WorkflowStepEntity entity) {
        WorkflowStepResponse response = new WorkflowStepResponse();
        response.setId(entity.getId());
        response.setInternalname(entity.getInternalname());
        response.setDisplayname(entity.getDisplayname());
        response.setSortorder(entity.getSortorder());
        return response;
    }

    private ResponseEntity<ErrorResponseDTO> workflowNotFound(String id) {
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            "Workflow with id '" + id + "' not found",
            "/api/workflows/" + id
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    private ResponseEntity<ErrorResponseDTO> workflowStepNotFound(String id) {
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            "Workflow step with id '" + id + "' not found",
            "/api/workflow-steps/" + id
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
