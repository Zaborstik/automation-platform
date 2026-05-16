package com.zaborstik.platform.agent.config;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.service.AgentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Playwright HTTP client and the stateless step-executor service.
 */
@Configuration
public class AgentConfig {

    @Bean
    public AgentClient agentClient(@Value("${platform.playwright.url:http://localhost:3000}") String playwrightUrl) {
        return new AgentClient(playwrightUrl);
    }

    @Bean
    public AgentService agentService(AgentClient agentClient) {
        return new AgentService(agentClient);
    }
}
