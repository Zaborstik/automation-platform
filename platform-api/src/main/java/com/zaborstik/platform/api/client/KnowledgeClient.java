package com.zaborstik.platform.api.client;

import com.zaborstik.platform.core.plan.Plan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP client used by platform-api to delegate plan generation to the
 * server-side {@code platform-knowledge} microservice.
 *
 * <p>This is the only outbound integration the api gateway has on the server
 * side. The URL is provided via {@code platform.knowledge.url} (compose
 * default: {@code http://platform-knowledge:8081}).
 */
@Component
public class KnowledgeClient {

    private final RestClient restClient;

    public KnowledgeClient(@Value("${platform.knowledge.url:http://localhost:8081}") String knowledgeUrl) {
        String baseUrl = knowledgeUrl.endsWith("/") ? knowledgeUrl.substring(0, knowledgeUrl.length() - 1) : knowledgeUrl;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Calls {@code POST /api/knowledge/generate-plan} and returns the produced
     * plan template (no DB ids assigned yet — those will be created by
     * {@link com.zaborstik.platform.api.service.PlanService}).
     */
    public Plan generatePlan(String userInput) {
        try {
            Plan plan = restClient.post()
                .uri("/api/knowledge/generate-plan")
                .body(Map.of("userInput", userInput))
                .retrieve()
                .body(Plan.class);
            if (plan == null) {
                throw new IllegalStateException("Knowledge service returned an empty body");
            }
            return plan;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to call knowledge service: " + ex.getMessage(), ex);
        }
    }

    public Duration timeout() {
        return Duration.ofSeconds(30);
    }
}
