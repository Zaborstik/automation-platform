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
     * Получает план по идентификатору.
     * 
     * TODO: Реализовать хранение планов и получение по ID.
     * 
     * @param id идентификатор плана
     * @return план выполнения
     * 
     * Gets plan by identifier.
     * 
     * TODO: Implement plan storage and retrieval by ID.
     * 
     * @param id plan identifier
     * @return execution plan
     */
    @GetMapping("/plan/{id}")
    public ResponseEntity<ErrorResponseDTO> getPlan(@PathVariable("id") String id) {
        // Пока не реализовано - планы не сохраняются
        // Not implemented yet - plans are not stored
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.NOT_IMPLEMENTED.value(),
            "Not Implemented",
            "Plan storage is not implemented yet. Plans are created on-demand.",
            "/api/execution/plan/" + id
        );
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
    }
}

