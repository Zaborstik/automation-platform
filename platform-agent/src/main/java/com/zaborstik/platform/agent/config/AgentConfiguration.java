package com.zaborstik.platform.agent.config;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.core.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Конфигурация для создания компонентов агента.
 * Упрощает создание AgentService с правильными зависимостями.
 */
public class AgentConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AgentConfiguration.class);

    private final String agentUrl;
    private final String applicationBaseUrl;
    private final boolean headless;
    private final Duration timeout;

    public AgentConfiguration(String agentUrl, String applicationBaseUrl, boolean headless) {
        this(agentUrl, applicationBaseUrl, headless, Duration.ofSeconds(30));
    }

    public AgentConfiguration(String agentUrl, String applicationBaseUrl, 
                             boolean headless, Duration timeout) {
        this.agentUrl = agentUrl;
        this.applicationBaseUrl = applicationBaseUrl;
        this.headless = headless;
        this.timeout = timeout;
    }

    /**
     * Создает AgentClient с настройками из конфигурации.
     */
    public AgentClient createAgentClient() {
        log.info("Creating AgentClient with URL: {}", agentUrl);
        return new AgentClient(agentUrl, timeout);
    }

    /**
     * Создает AgentService с настройками из конфигурации.
     */
    public AgentService createAgentService(Resolver resolver) {
        log.info("Creating AgentService with baseUrl: {}, headless: {}", 
            applicationBaseUrl, headless);
        AgentClient client = createAgentClient();
        return new AgentService(client, resolver, applicationBaseUrl, headless);
    }

    /**
     * Проверяет доступность агента.
     */
    public boolean checkAgentAvailability() {
        try {
            AgentClient client = createAgentClient();
            boolean available = client.isAvailable();
            log.info("Agent availability check: {}", available);
            return available;
        } catch (Exception e) {
            log.error("Failed to check agent availability", e);
            return false;
        }
    }

    // Getters
    public String getAgentUrl() {
        return agentUrl;
    }

    public String getApplicationBaseUrl() {
        return applicationBaseUrl;
    }

    public boolean isHeadless() {
        return headless;
    }

    public Duration getTimeout() {
        return timeout;
    }
}

