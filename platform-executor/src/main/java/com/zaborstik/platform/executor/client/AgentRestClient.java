package com.zaborstik.platform.executor.client;

import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.ExecuteStepRequest;
import com.zaborstik.platform.agent.dto.InitSessionRequest;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.PlanStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Objects;

/**
 * HTTP client used by {@code platform-executor} to drive the local
 * {@code platform-agent} microservice. Lives on the same machine as the
 * executor, so latency is negligible.
 */
@Component
public class AgentRestClient {

    private final RestClient restClient;

    public AgentRestClient(@Value("${platform.agent.url:http://localhost:7071}") String agentUrl) {
        String baseUrl = agentUrl.endsWith("/") ? agentUrl.substring(0, agentUrl.length() - 1) : agentUrl;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public AgentResponse initializeSession(String baseUrl, boolean headless) {
        try {
            AgentResponse response = restClient.post()
                .uri("/api/agent/session")
                .body(new InitSessionRequest(baseUrl, headless))
                .retrieve()
                .body(AgentResponse.class);
            if (response == null) {
                throw new IllegalStateException("Empty response from agent /session");
            }
            return response;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to initialize agent: " + ex.getMessage(), ex);
        }
    }

    public StepExecutionResult executeStep(PlanStep step, String operation,
                                            Map<String, String> resolvedSelectors,
                                            int stepIndex) {
        Objects.requireNonNull(step, "step");
        Objects.requireNonNull(operation, "operation");
        ExecuteStepRequest body = new ExecuteStepRequest();
        body.setStep(step);
        body.setOperation(operation);
        body.setResolvedSelectors(resolvedSelectors);
        body.setStepIndex(stepIndex);
        try {
            StepExecutionResult result = restClient.post()
                .uri("/api/agent/execute-step")
                .body(body)
                .retrieve()
                .body(StepExecutionResult.class);
            if (result == null) {
                throw new IllegalStateException("Empty response from agent /execute-step");
            }
            return result;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Agent execute-step failed: " + ex.getMessage(), ex);
        }
    }

    public AgentResponse closeSession() {
        try {
            AgentResponse response = restClient.delete()
                .uri("/api/agent/session")
                .retrieve()
                .body(AgentResponse.class);
            return response != null ? response : AgentResponse.success("closed", Map.of(), 0L);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to close agent: " + ex.getMessage(), ex);
        }
    }
}
