package com.zaborstik.platform.api.config;

import com.zaborstik.platform.api.resolver.DatabaseResolver;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.resolver.Resolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Spring для настройки ExecutionEngine и Resolver.
 * 
 * Использует DatabaseResolver для работы с БД через JPA репозитории.
 * 
 * Spring configuration for ExecutionEngine and Resolver.
 * 
 * Uses DatabaseResolver for working with database through JPA repositories.
 */
@Configuration
public class PlatformConfiguration {

    @Bean
    public Resolver resolver(DatabaseResolver databaseResolver) {
        // Используем DatabaseResolver, который работает с БД
        // Use DatabaseResolver that works with database
        return databaseResolver;
    }

    @Bean
    public ExecutionEngine executionEngine(Resolver resolver) {
        return new ExecutionEngine(resolver);
    }
}

