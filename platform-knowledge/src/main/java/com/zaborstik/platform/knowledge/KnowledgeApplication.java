package com.zaborstik.platform.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entrypoint of the knowledge microservice.
 *
 * <p>Runs on the server side and exposes a REST endpoint for plan generation
 * from a natural-language user request. The platform-api gateway calls this
 * service via HTTP whenever a new plan needs to be produced.
 */
@SpringBootApplication
public class KnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}
