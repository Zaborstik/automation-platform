package com.zaborstik.platform.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entrypoint of the local executor microservice.
 *
 * <p>Runs on the user's machine. Bridges the chat overlay (which talks to
 * the executor over HTTP) with the remote {@code platform-api} (REST) and
 * the local {@code platform-agent} (REST).
 */
@SpringBootApplication
public class ExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutorApplication.class, args);
    }
}
