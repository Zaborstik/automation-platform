package com.zaborstik.platform.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс Spring Boot приложения для Platform API.
 * Предоставляет REST API для работы с execution-платформой.
 */
@SpringBootApplication
public class PlatformApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}

