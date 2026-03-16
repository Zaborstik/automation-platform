package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentHealthCheckTest {

    @Mock
    private AgentClient agentClient;

    @Test
    void isHealthyShouldReturnTrueWhenAgentAvailable() {
        when(agentClient.isAvailable()).thenReturn(true);
        AgentHealthCheck check = new AgentHealthCheck(agentClient);

        assertTrue(check.isHealthy());
    }

    @Test
    void isHealthyShouldReturnFalseWhenAgentUnavailable() {
        when(agentClient.isAvailable()).thenReturn(false);
        AgentHealthCheck check = new AgentHealthCheck(agentClient);

        assertFalse(check.isHealthy());
    }

    @Test
    void waitForHealthyShouldReturnTrueWhenAgentBecomesHealthy() {
        when(agentClient.isAvailable()).thenReturn(false, false, true);
        AgentHealthCheck check = new AgentHealthCheck(agentClient);

        assertTrue(check.waitForHealthy(200, 10));
    }

    @Test
    void waitForHealthyShouldReturnFalseOnTimeout() {
        when(agentClient.isAvailable()).thenReturn(false);
        AgentHealthCheck check = new AgentHealthCheck(agentClient);

        assertFalse(check.waitForHealthy(40, 10));
    }
}
