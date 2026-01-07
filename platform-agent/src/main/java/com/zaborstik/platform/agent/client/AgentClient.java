package com.zaborstik.platform.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.agent.dto.AgentCommand;
import com.zaborstik.platform.agent.dto.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Клиент для взаимодействия с Playwright сервером через HTTP.
 * Отправляет команды агенту и получает результаты выполнения.
 * 
 * Client for interacting with Playwright server via HTTP.
 * Sends commands to agent and receives execution results.
 */
public class AgentClient {
    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);
    
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration defaultTimeout;

    public AgentClient(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(30));
    }

    public AgentClient(String baseUrl, Duration defaultTimeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultTimeout = defaultTimeout;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Выполняет команду через агента.
     * 
     * @param command команда для выполнения
     * @return ответ от агента
     * @throws AgentException если произошла ошибка при выполнении
     * 
     * Executes command through agent.
     * 
     * @param command command to execute
     * @return response from agent
     * @throws AgentException if error occurred during execution
     */
    public AgentResponse execute(AgentCommand command) throws AgentException {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Executing command: {}", command);
            
            String requestBody = objectMapper.writeValueAsString(command);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/execute"))
                .header("Content-Type", "application/json")
                .timeout(defaultTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long executionTime = System.currentTimeMillis() - startTime;

            if (response.statusCode() != 200) {
                String error = String.format("Agent returned status %d: %s", 
                    response.statusCode(), response.body());
                log.error(error);
                return AgentResponse.failure(error, executionTime);
            }

            AgentResponse agentResponse = objectMapper.readValue(response.body(), AgentResponse.class);
            log.debug("Command executed successfully in {}ms", executionTime);
            return agentResponse;

        } catch (IOException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String error = "Failed to communicate with agent: " + e.getMessage();
            log.error(error, e);
            return AgentResponse.failure(error, executionTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long executionTime = System.currentTimeMillis() - startTime;
            String error = "Command execution interrupted: " + e.getMessage();
            log.error(error, e);
            return AgentResponse.failure(error, executionTime);
        }
    }

    /**
     * Проверяет доступность агента.
     * 
     * @return true если агент доступен
     * 
     * Checks agent availability.
     * 
     * @return true if agent is available
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Agent health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Инициализирует браузер (открывает новый контекст).
     * 
     * @param baseUrl базовый URL приложения
     * @param headless запускать ли в headless режиме
     * @return ответ от агента
     * 
     * Initializes browser (opens new context).
     * 
     * @param baseUrl application base URL
     * @param headless whether to run in headless mode
     * @return response from agent
     */
    public AgentResponse initialize(String baseUrl, boolean headless) throws AgentException {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "baseUrl", baseUrl,
                "headless", headless
            ));
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.baseUrl + "/initialize"))
                .header("Content-Type", "application/json")
                .timeout(defaultTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String error = String.format("Failed to initialize agent: status %d", response.statusCode());
                log.error(error);
                return AgentResponse.failure(error, 0);
            }

            return objectMapper.readValue(response.body(), AgentResponse.class);

        } catch (Exception e) {
            String error = "Failed to initialize agent: " + e.getMessage();
            log.error(error, e);
            return AgentResponse.failure(error, 0);
        }
    }

    /**
     * Закрывает браузер и освобождает ресурсы.
     * 
     * Closes browser and releases resources.
     */
    public AgentResponse close() throws AgentException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/close"))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return AgentResponse.failure("Failed to close agent", 0);
            }

            return objectMapper.readValue(response.body(), AgentResponse.class);

        } catch (Exception e) {
            log.error("Failed to close agent", e);
            return AgentResponse.failure("Failed to close agent: " + e.getMessage(), 0);
        }
    }
}

