package com.zaborstik.platform.executor.controller;

import com.zaborstik.platform.executor.dto.LocalRunRequest;
import com.zaborstik.platform.executor.dto.LocalRunResponse;
import com.zaborstik.platform.executor.service.LocalRunRecord;
import com.zaborstik.platform.executor.service.LocalRunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.NoSuchElementException;

/**
 * REST entry point of the local executor microservice.
 *
 * <p>The chat overlay (and any other local UI / CLI) calls these endpoints to
 * kick off plan generation and execution, then polls
 * {@code GET /local/status/{runId}} for progress.
 *
 * <p>{@code @CrossOrigin} is wide open on purpose: only locally-running UIs
 * can reach the executor (the listener should never be exposed to the
 * network), so allowing any origin keeps Tauri webviews happy.
 */
@RestController
@RequestMapping("/local")
@CrossOrigin(origins = "*")
public class LocalExecutorController {

    private final LocalRunService runService;

    public LocalExecutorController(LocalRunService runService) {
        this.runService = runService;
    }

    /**
     * Full flow: ask the server to generate a plan from user input, then run it
     * locally and stream progress back via {@code /local/status}.
     */
    @PostMapping("/run")
    public ResponseEntity<LocalRunResponse> run(@Valid @RequestBody LocalRunRequest request) {
        LocalRunRecord record = runService.runFromUserInput(
            request.getUserInput(),
            request.getBrowserBaseUrl(),
            request.getHeadless()
        );
        return ResponseEntity.accepted().body(LocalRunResponse.from(record));
    }

    /**
     * Runs an already existing plan stored on the server.
     */
    @PostMapping("/run-plan/{planId}")
    public ResponseEntity<LocalRunResponse> runPlan(@PathVariable("planId") String planId,
                                                    @RequestBody(required = false) LocalRunRequest request) {
        String browserBaseUrl = request != null ? request.getBrowserBaseUrl() : null;
        Boolean headless = request != null ? request.getHeadless() : null;
        try {
            LocalRunRecord record = runService.runExistingPlan(planId, browserBaseUrl, headless);
            return ResponseEntity.accepted().body(LocalRunResponse.from(record));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/status/{runId}")
    public ResponseEntity<LocalRunResponse> status(@PathVariable("runId") String runId) {
        return runService.getRun(runId)
            .map(LocalRunResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
