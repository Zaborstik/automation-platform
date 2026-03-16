package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class AgentHealthCheck {
    private static final Logger log = LoggerFactory.getLogger(AgentHealthCheck.class);

    private final AgentClient agentClient;

    public AgentHealthCheck(AgentClient agentClient) {
        this.agentClient = Objects.requireNonNull(agentClient, "agentClient cannot be null");
    }

    public boolean isHealthy() {
        return agentClient.isAvailable();
    }

    public boolean waitForHealthy(long timeoutMs, long pollIntervalMs) {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("timeoutMs must be >= 0");
        }
        if (pollIntervalMs <= 0) {
            throw new IllegalArgumentException("pollIntervalMs must be > 0");
        }

        long startedAt = System.currentTimeMillis();
        int attempt = 1;
        while (System.currentTimeMillis() - startedAt <= timeoutMs) {
            boolean healthy = agentClient.isAvailable();
            log.info("Health check attempt {} result={}", attempt, healthy ? "healthy" : "unhealthy");
            if (healthy) {
                return true;
            }

            attempt++;
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(pollIntervalMs));
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Interrupted while waiting for healthy agent");
                return false;
            }
        }

        return false;
    }
}
