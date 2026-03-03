package com.zaborstik.platform;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.ActionType;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import com.zaborstik.platform.executor.PlanExecutionResult;
import com.zaborstik.platform.executor.PlanExecutor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Простой CLI-вход для manual-run MVP:
 * - собирает in-memory Resolver (EntityType, ActionType, Action, UIBinding)
 * - создаёт простой Plan с шагами (новая модель: plan → plan_step → plan_step_action)
 * - передаёт его в PlanExecutor, который управляет UI-агентом.
 * пвапвва
 * Это пример, а не продовый bootstrap.
 */
public class Main {
    public static void main(String[] args) {


        // 1. Конфигурируем resolver
        InMemoryResolver resolver = new InMemoryResolver();

        EntityType building = EntityType.of("Building", "Здание");
        resolver.registerEntityType(building);

        ActionType clickType = new ActionType("action-type-click", "click", "Клик");
        resolver.registerActionType(clickType);

        Action orderEgrn = Action.of(
            "order_egrn_extract",
            "Заказать выписку ЕГРН",
            "order_egrn_extract",
            "Заказать выписку ЕГРН по зданию",
            clickType.id()
        );
        resolver.registerAction(orderEgrn);
        resolver.registerActionApplicableToEntityType(orderEgrn.id(), building.id());

        UIBinding binding = new UIBinding(
            orderEgrn.id(),
            "button[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            Map.of()
        );
        resolver.registerUIBinding(binding);

        // 2. Конструируем план (новая модель: id, workflowId, workflowStepInternalName, stoppedAtPlanStepId, target, explanation, steps)
        String entityId = "93939";
        String planId = UUID.randomUUID().toString();

        List<PlanStep> steps = List.of(
            new PlanStep(
                "step-1", planId, "workflow-plan", "open_page",
                building.id(), "/buildings/" + entityId, 0,
                "Открываю карточку здания",
                List.of()
            ),
            new PlanStep(
                "step-2", planId, "workflow-plan", "explain",
                building.id(), null, 1,
                "Навожу курсор на действие 'Заказать выписку ЕГРН'",
                List.of()
            ),
            new PlanStep(
                "step-3", planId, "workflow-plan", "hover",
                building.id(), "action(" + orderEgrn.id() + ")", 2,
                "Подсвечиваю действие",
                List.of(new PlanStepAction(orderEgrn.id(), null))
            ),
            new PlanStep(
                "step-4", planId, "workflow-plan", "click",
                building.id(), "action(" + orderEgrn.id() + ")", 3,
                "Запускаю заказ выписки",
                List.of(new PlanStepAction(orderEgrn.id(), null))
            ),
            new PlanStep(
                "step-5", planId, "workflow-plan", "wait",
                building.id(), "result", 4,
                "Жду результата выполнения действия",
                List.of()
            )
        );

        Plan plan = new Plan(
            planId,
            "workflow-plan",
            "in_progress",
            "step-1",
            "Здание " + entityId,
            "Заказ выписки ЕГРН",
            steps
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
                entry.getStep().displayName(),
                entry.getResult().isSuccess(),
                entry.getResult().getMessage(),
                entry.getResult().getError()
            );
        });
    }
}
