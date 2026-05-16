package com.zaborstik.platform.agent.controller;

import com.zaborstik.platform.agent.client.AgentException;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.ExecuteStepRequest;
import com.zaborstik.platform.agent.dto.InitSessionRequest;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Thin REST facade over the Playwright sidecar. Called from the local
 * {@code platform-executor} on the same host.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Starts (or re-starts) the browser session.
     */
    @PostMapping("/session")
    public ResponseEntity<AgentResponse> initSession(@RequestBody(required = false) InitSessionRequest request) {
        InitSessionRequest body = request != null ? request : new InitSessionRequest();
        String baseUrl = body.getBaseUrl() != null ? body.getBaseUrl() : "";
        boolean headless = body.getHeadless() != null ? body.getHeadless() : false;
        try {
            AgentResponse response = agentService.initializeSession(baseUrl, headless);
            HttpStatus status = response.success() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY;
            return ResponseEntity.status(status).body(response);
        } catch (AgentException ex) {
            log.error("Agent initialization failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(AgentResponse.failure("Agent initialization failed: " + ex.getMessage(), 0));
        }
    }

    /**
     * Runs a single fully-resolved plan step against the browser.
     */
    @PostMapping("/execute-step")
    public ResponseEntity<StepExecutionResult> executeStep(@Valid @RequestBody ExecuteStepRequest request) {
        Map<String, String> selectors = request.getResolvedSelectors() != null
            ? request.getResolvedSelectors()
            : Map.of();
        StepExecutionResult result = agentService.executeStep(
            request.getStep(),
            request.getOperation(),
            selectors,
            request.getStepIndex()
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Closes the browser session and releases resources.
     */
    @DeleteMapping("/session")
    public ResponseEntity<AgentResponse> closeSession() {
        try {
            AgentResponse response = agentService.closeSession();
            return ResponseEntity.ok(response);
        } catch (AgentException ex) {
            log.error("Agent close failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(AgentResponse.failure("Agent close failed: " + ex.getMessage(), 0));
        }
    }
}
