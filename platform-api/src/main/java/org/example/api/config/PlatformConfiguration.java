package org.example.api.config;

import org.example.core.ExecutionEngine;
import org.example.core.domain.Action;
import org.example.core.domain.EntityType;
import org.example.core.domain.UIBinding;
import org.example.core.resolver.InMemoryResolver;
import org.example.core.resolver.Resolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

/**
 * Конфигурация Spring для настройки ExecutionEngine и Resolver.
 * 
 * В MVP используется InMemoryResolver с предзаполненными данными.
 * В будущем это может быть заменено на репозиторий с БД.
 */
@Configuration
public class PlatformConfiguration {

    @Bean
    public Resolver resolver() {
        InMemoryResolver resolver = new InMemoryResolver();
        
        // Регистрируем примеры EntityType
        registerExampleEntityTypes(resolver);
        
        // Регистрируем примеры Action
        registerExampleActions(resolver);
        
        // Регистрируем примеры UIBinding
        registerExampleUIBindings(resolver);
        
        return resolver;
    }

    @Bean
    public ExecutionEngine executionEngine(Resolver resolver) {
        return new ExecutionEngine(resolver);
    }

    /**
     * Регистрирует примеры типов сущностей.
     */
    private void registerExampleEntityTypes(InMemoryResolver resolver) {
        resolver.registerEntityType(new EntityType(
            "Building",
            "Здание",
            Map.of("description", "Тип сущности для работы со зданиями")
        ));

        resolver.registerEntityType(new EntityType(
            "Contract",
            "Договор",
            Map.of("description", "Тип сущности для работы с договорами")
        ));
    }

    /**
     * Регистрирует примеры действий.
     */
    private void registerExampleActions(InMemoryResolver resolver) {
        resolver.registerAction(new Action(
            "order_egrn_extract",
            "Заказать выписку из ЕГРН",
            "Заказывает выписку из ЕГРН для указанного здания",
            Set.of("Building"),
            Map.of("category", "document", "requiresAuth", true)
        ));

        resolver.registerAction(new Action(
            "close_contract",
            "Закрыть договор",
            "Закрывает указанный договор",
            Set.of("Contract"),
            Map.of("category", "workflow", "irreversible", true)
        ));

        resolver.registerAction(new Action(
            "assign_owner",
            "Назначить владельца",
            "Назначает владельца для указанного здания",
            Set.of("Building"),
            Map.of("category", "management", "requiresPermission", "admin")
        ));
    }

    /**
     * Регистрирует примеры UI-привязок.
     */
    private void registerExampleUIBindings(InMemoryResolver resolver) {
        resolver.registerUIBinding(new UIBinding(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            Map.of("highlight", true, "waitAfterClick", 2000)
        ));

        resolver.registerUIBinding(new UIBinding(
            "close_contract",
            "//button[contains(@class, 'close-contract-btn')]",
            UIBinding.SelectorType.XPATH,
            Map.of("highlight", true, "confirmRequired", true)
        ));

        resolver.registerUIBinding(new UIBinding(
            "assign_owner",
            "[data-action='assign_owner']",
            UIBinding.SelectorType.CSS,
            Map.of("highlight", true, "modalOpens", true)
        ));
    }
}

