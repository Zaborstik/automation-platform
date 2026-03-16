package com.zaborstik.platform.api.controller;

import com.zaborstik.platform.api.dto.ActionResponse;
import com.zaborstik.platform.api.dto.CreateActionRequest;
import com.zaborstik.platform.api.dto.ErrorResponseDTO;
import com.zaborstik.platform.api.dto.UpdateActionRequest;
import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.service.ActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actions")
@Tag(name = "Actions")
public class ActionController {

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @GetMapping
    @Operation(summary = "List actions")
    @ApiResponse(responseCode = "200", description = "Actions returned")
    public ResponseEntity<List<ActionResponse>> listActions(
        @RequestParam(value = "entityTypeId", required = false) String entityTypeId
    ) {
        List<ActionEntity> entities = entityTypeId == null || entityTypeId.isBlank()
            ? actionService.listAll()
            : actionService.findByEntityType(entityTypeId);
        return ResponseEntity.ok(entities.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get action by id")
    @ApiResponse(responseCode = "200", description = "Action found")
    @ApiResponse(responseCode = "404", description = "Action not found")
    public ResponseEntity<?> getById(@PathVariable("id") String id) {
        return actionService.getById(id)
            .<ResponseEntity<?>>map(action -> ResponseEntity.ok(toResponse(action)))
            .orElseGet(() -> {
                ErrorResponseDTO error = new ErrorResponseDTO(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    "Action with id '" + id + "' not found",
                    "/api/actions/" + id
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            });
    }

    @PostMapping
    @Operation(summary = "Create action")
    @ApiResponse(responseCode = "201", description = "Action created")
    public ResponseEntity<ActionResponse> create(@Valid @RequestBody CreateActionRequest request) {
        ActionEntity created = actionService.create(
            request.getDisplayname(),
            request.getInternalname(),
            request.getDescription(),
            request.getActionTypeId(),
            request.getMetaValue()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update action")
    @ApiResponse(responseCode = "200", description = "Action updated")
    @ApiResponse(responseCode = "404", description = "Action not found")
    public ResponseEntity<?> update(@PathVariable("id") String id, @RequestBody UpdateActionRequest request) {
        try {
            ActionEntity updated = actionService.update(
                id,
                request.getDisplayname(),
                request.getDescription(),
                request.getMetaValue()
            );
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Action not found:")) {
                ErrorResponseDTO error = new ErrorResponseDTO(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    "Action with id '" + id + "' not found",
                    "/api/actions/" + id
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            throw ex;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete action")
    @ApiResponse(responseCode = "204", description = "Action deleted")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        actionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ActionResponse toResponse(ActionEntity entity) {
        ActionResponse response = new ActionResponse();
        response.setId(entity.getId());
        response.setDisplayname(entity.getDisplayname());
        response.setInternalname(entity.getInternalname());
        response.setDescription(entity.getDescription());
        response.setMetaValue(entity.getMetaValue());
        response.setActionTypeId(entity.getActionType() != null ? entity.getActionType().getId() : null);
        response.setCreatedTime(entity.getCreatedTime());
        response.setUpdatedTime(entity.getUpdatedTime());
        return response;
    }
}
