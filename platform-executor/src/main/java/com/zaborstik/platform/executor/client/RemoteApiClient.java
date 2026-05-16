package com.zaborstik.platform.executor.client;

import com.zaborstik.platform.executor.client.api.ActionDto;
import com.zaborstik.platform.executor.client.api.PlanDto;
import com.zaborstik.platform.executor.client.api.PlanRunStartDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * HTTP client used by {@code platform-executor} to talk to the remote
 * {@code platform-api} gateway.
 *
 * <p>Encapsulates plan retrieval, dictionary lookups (actions / entity types
 * used by the RemoteResolver), plan-run lifecycle control and per-step result
 * reporting.
 */
@Component
public class RemoteApiClient {

    private final RestClient restClient;

    public RemoteApiClient(@Value("${platform.api.url:http://localhost:8080}") String apiUrl) {
        String baseUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public Optional<PlanDto> fetchPlan(String planId) {
        Objects.requireNonNull(planId, "planId");
        try {
            PlanDto plan = restClient.get()
                .uri("/api/plans/{id}", planId)
                .retrieve()
                .body(PlanDto.class);
            return Optional.ofNullable(plan);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to fetch plan " + planId + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Asks the api to materialise a plan out of a free-form user input.
     * Internally the api delegates to {@code platform-knowledge}.
     */
    public PlanDto createPlanFromRequest(String userInput) {
        Objects.requireNonNull(userInput, "userInput");
        try {
            PlanDto plan = restClient.post()
                .uri("/api/plans/from-request")
                .body(Map.of("userInput", userInput))
                .retrieve()
                .body(PlanDto.class);
            if (plan == null) {
                throw new IllegalStateException("Empty plan returned from /api/plans/from-request");
            }
            return plan;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to create plan from request: " + ex.getMessage(), ex);
        }
    }

    public Optional<ActionDto> fetchAction(String actionId) {
        Objects.requireNonNull(actionId, "actionId");
        try {
            ActionDto action = restClient.get()
                .uri("/api/actions/{id}", actionId)
                .retrieve()
                .body(ActionDto.class);
            return Optional.ofNullable(action);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to fetch action " + actionId + ": " + ex.getMessage(), ex);
        }
    }

    public List<ActionDto> fetchActionsByEntityType(String entityTypeId) {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        try {
            List<ActionDto> actions = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/actions")
                    .queryParam("entityTypeId", entityTypeId)
                    .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
            return actions != null ? actions : List.of();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to list actions for " + entityTypeId + ": " + ex.getMessage(), ex);
        }
    }

    public PlanRunStartDto startRun(String planId) {
        Objects.requireNonNull(planId, "planId");
        try {
            PlanRunStartDto run = restClient.post()
                .uri("/api/plans/{id}/runs", planId)
                .retrieve()
                .body(PlanRunStartDto.class);
            if (run == null) {
                throw new IllegalStateException("Empty run response from /api/plans/" + planId + "/runs");
            }
            return run;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to start run for plan " + planId + ": " + ex.getMessage(), ex);
        }
    }

    public void reportStepResult(String planId, String stepId, String planResultId,
                                  Map<String, Object> body) {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(stepId, "stepId");
        Objects.requireNonNull(planResultId, "planResultId");
        try {
            restClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/plans/{planId}/steps/{stepId}/result")
                    .queryParam("planResultId", planResultId)
                    .build(planId, stepId))
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to report step " + stepId + " of plan " + planId
                + ": " + ex.getMessage(), ex);
        }
    }

    public PlanRunStartDto finishRun(String planId, String planResultId, boolean success,
                                      int totalSteps, int failedSteps,
                                      Instant startedTime, Instant finishedTime) {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(planResultId, "planResultId");
        try {
            PlanRunStartDto run = restClient.post()
                .uri("/api/plans/{id}/runs/finish", planId)
                .body(Map.of(
                    "planResultId", planResultId,
                    "success", success,
                    "totalSteps", totalSteps,
                    "failedSteps", failedSteps,
                    "startedTime", startedTime != null ? startedTime.toString() : Instant.now().toString(),
                    "finishedTime", finishedTime != null ? finishedTime.toString() : Instant.now().toString()
                ))
                .retrieve()
                .body(PlanRunStartDto.class);
            if (run == null) {
                throw new IllegalStateException("Empty finish run response for plan " + planId);
            }
            return run;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to finish run for plan " + planId + ": " + ex.getMessage(), ex);
        }
    }
}
