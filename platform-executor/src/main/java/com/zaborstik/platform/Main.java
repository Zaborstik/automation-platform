package com.zaborstik.platform;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import com.zaborstik.platform.executor.PlanExecutionResult;
import com.zaborstik.platform.executor.PlanExecutor;

import java.util.List;

/**
 * Простой CLI-вход для manual-run MVP:
 * - собирает in-memory Resolver (EntityType, Action, UIBinding)
 * - создаёт простой Plan (линейный)
 * - передаёт его в PlanExecutor, который управляет UI-агентом.
 *
 * Это пример, а не продовый bootstrap.
 */
public class Main {
    public static void main(String[] args) {
        // 1. Конфигурируем resolver c одной сущностью и действием
        InMemoryResolver resolver = new InMemoryResolver();

        EntityType building = new EntityType("Building", "Здание", java.util.Map.of());
        resolver.registerEntityType(building);

        Action orderEgrn = new Action(
            "order_egrn_extract",
            "Заказать выписку ЕГРН",
            "Заказать выписку ЕГРН по зданию",
            java.util.Set.of(building.id()),
            java.util.Map.of()
        );
        resolver.registerAction(orderEgrn);

        // Привязка действия к UI (селектор условный, адаптируется под конкретное приложение)
        UIBinding binding = new UIBinding(
            orderEgrn.id(),
            "button[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            java.util.Map.of()
        );
        resolver.registerUIBinding(binding);

        // 2. Конструируем план руками (в бою он придёт из core/ExecutionEngine)
        String entityId = "93939";
        Plan plan = new Plan(
            building.id(),
            entityId,
            orderEgrn.id(),
            List.of(
                PlanStep.openPage("/buildings/" + entityId, "Открываю карточку здания"),
                PlanStep.explain("Навожу курсор на действие 'Заказать выписку ЕГРН'"),
                PlanStep.hover(orderEgrn.id(), "Подсвечиваю действие"),
                PlanStep.click(orderEgrn.id(), "Запускаю заказ выписки"),
                PlanStep.wait("result", "Жду результата выполнения действия")
            )
        );

        // 3. Поднимаем клиента к Playwright-агенту
        String agentBaseUrl = "http://localhost:3000"; // URL Playwright-сервера
        AgentClient agentClient = new AgentClient(agentBaseUrl);

        String appBaseUrl = "http://localhost:8080"; // URL бизнес-приложения
        boolean headless = false;
        AgentService agentService = new AgentService(agentClient, resolver, appBaseUrl, headless);

        // 4. Исполняем план через executor
        PlanExecutor executor = new PlanExecutor(agentService);
        PlanExecutionResult result = executor.execute(plan);

        System.out.println("=== Plan execution finished ===");
        System.out.println("Plan id: " + result.getPlanId());
        System.out.println("Success: " + result.isSuccess());
        result.getLogEntries().forEach(entry -> {
            System.out.printf(
                "[%s] #%d %s -> success=%s, msg=%s, err=%s%n",
                entry.getLoggedAt(),
                entry.getStepIndex(),
                entry.getStep(),
                entry.getResult().isSuccess(),
                entry.getResult().getMessage(),
                entry.getResult().getError()
            );
        });
    }
}
