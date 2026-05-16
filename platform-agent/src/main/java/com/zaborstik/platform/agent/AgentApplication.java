package com.zaborstik.platform.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entrypoint of the local agent microservice.
 *
 * <p>Runs on the user's machine alongside the Node Playwright sidecar.
 * Listens on port 7071 by default and exposes a per-step REST API consumed
 * by {@code platform-executor} on the same host.
 */
@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
