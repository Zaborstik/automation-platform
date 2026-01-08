package com.zaborstik.platform.api.controller;

import jakarta.validation.Valid;
import com.zaborstik.platform.api.dto.ErrorResponseDTO;
import com.zaborstik.platform.api.dto.ExecutionRequestDTO;
import com.zaborstik.platform.api.dto.PlanDTO;
import com.zaborstik.platform.api.service.ExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер для работы с выполнением действий.
 * 
 * Эндпоинты:
 * - POST /api/execution/plan - создание плана выполнения
 * - GET /api/execution/plan/{id} - получение плана (для будущей реализации)
 * 
 * REST controller for action execution.
 * 
 * Endpoints:
 * - POST /api/execution/plan - create execution plan
 * - GET /api/execution/plan/{id} - get plan (for future implementation)
 */
@RestController
@RequestMapping("/api/execution")
public class ExecutionController {
    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * Создает план выполнения для указанного действия.
     * 
     * Пример запроса:
     * POST /api/execution/plan
     * {
     *   "entity": "Building",
     *   "entityId": "93939",
     *   "action": "order_egrn_extract",
     *   "parameters": {}
     * }
     * 
     * @param requestDTO запрос на выполнение действия
     * @return план выполнения
     * 
     * Creates execution plan for the specified action.
     * 
     * Request example:
     * POST /api/execution/plan
     * {
     *   "entity": "Building",
     *   "entityId": "93939",
     *   "action": "order_egrn_extract",
     *   "parameters": {}
     * }
     * 
     * @param requestDTO action execution request
     * @return execution plan
     */
    @PostMapping("/plan")
    public ResponseEntity<PlanDTO> createPlan(@Valid @RequestBody ExecutionRequestDTO requestDTO) {
        PlanDTO plan = executionService.createPlan(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    /**
     * Получает план по идентификатору из БД.
     * 
     * Gets plan by identifier from database.
     * 
     * @param id идентификатор плана / plan identifier
     * @return план выполнения или 404, если не найден / execution plan or 404 if not found
     */
    @GetMapping("/plan/{id}")
    public ResponseEntity<?> getPlan(@PathVariable("id") String id) {
        return executionService.getPlan(id)
            .<ResponseEntity<?>>map(plan -> ResponseEntity.ok(plan))
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

