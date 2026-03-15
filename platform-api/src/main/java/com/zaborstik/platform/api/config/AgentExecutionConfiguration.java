package com.zaborstik.platform.api.config;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.core.resolver.Resolver;
import com.zaborstik.platform.executor.PlanExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация browser executor через Playwright-агент.
 */
@Configuration
public class AgentExecutionConfiguration {

    @Bean(destroyMethod = "")
    public AgentClient agentClient(@Value("${platform.agent.server-url:http://localhost:3000}") String serverUrl) {
        return new AgentClient(serverUrl);
    }

    @Bean(destroyMethod = "")
    public AgentService agentService(AgentClient agentClient,
                                     Resolver resolver,
                                     @Value("${platform.agent.base-url:http://localhost:8080}") String baseUrl,
                                     @Value("${platform.agent.headless:false}") boolean headless) {
        return new AgentService(agentClient, resolver, baseUrl, headless);
    }

    @Bean
    public PlanExecutor planExecutor(AgentService agentService) {
        return new PlanExecutor(agentService);
    }
}
