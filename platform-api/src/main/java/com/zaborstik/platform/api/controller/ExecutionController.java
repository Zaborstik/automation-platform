package com.zaborstik.platform.api.controller;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.dto.ErrorResponseDTO;
import com.zaborstik.platform.api.service.ExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер для выполнения действий.
 * Запрос/ответ — универсальный EntityDTO (различие по tableName).
 */
@RestController
@RequestMapping("/api/execution")
public class ExecutionController {
    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * Создает план. Тело: EntityDTO с tableName="execution_request", в data: entity, entityId, action, parameters.
     */
    @PostMapping("/plan")
    public ResponseEntity<EntityDTO> createPlan(@Valid @RequestBody EntityDTO request) {
        EntityDTO plan = executionService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    /**
     * Получает план по id. Ответ: EntityDTO с tableName="plans".
     */
    @GetMapping("/plan/{id}")
    public ResponseEntity<?> getPlan(@PathVariable("id") String id) {
        return executionService.getPlan(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    ErrorResponseDTO error = new ErrorResponseDTO(
                            HttpStatus.NOT_FOUND.value(),
                            "Not Found",
                            "Plan with id '" + id + "' not found",
                            "/api/execution/plan/" + id
                    );
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                });
    }
}
