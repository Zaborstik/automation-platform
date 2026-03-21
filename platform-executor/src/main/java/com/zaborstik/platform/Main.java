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
 * - создаёт Plan: у шагов {@code workflow_step_internalname = new} (ЖЦ), тип UI-операции — {@code action.internalname} через {@code plan_step_action}
 * - передаёт план в {@link PlanExecutor}.
 *
 * Это пример, а не продовый bootstrap.
 */
public class Main {
    public static void main(String[] args) {

        InMemoryResolver resolver = new InMemoryResolver();

        EntityType building = EntityType.of("Building", "Здание");
        resolver.registerEntityType(building);

        ActionType clickType = new ActionType("action-type-click", "click", "Клик");
        resolver.registerActionType(clickType);

        Action actOpen = Action.of("act_open", "Открыть страницу", "open_page", "Переход по URL", clickType.id());
        Action actExplain = Action.of("act_explain", "Пояснение", "explain", "Пояснение для агента", clickType.id());
        Action actHover = Action.of("act_hover", "Наведение", "hover", "Наведение на элемент", clickType.id());
        Action actClick = Action.of("act_click_order", "Клик заказа", "click", "Клик по кнопке заказа", clickType.id());
        Action actWait = Action.of("act_wait", "Ожидание", "wait", "Ожидание результата", clickType.id());

        resolver.registerAction(actOpen);
        resolver.registerAction(actExplain);
        resolver.registerAction(actHover);
        resolver.registerAction(actClick);
        resolver.registerAction(actWait);

        String selector = "button[data-action='order_egrn_extract']";
        resolver.registerUIBinding(new UIBinding(actHover.id(), selector, UIBinding.SelectorType.CSS, Map.of()));
        resolver.registerUIBinding(new UIBinding(actClick.id(), selector, UIBinding.SelectorType.CSS, Map.of()));

        String entityId = "93939";
        String planId = UUID.randomUUID().toString();
        String wfPlanStep = "wf-plan-step";
        String listUrl = "http://localhost:8080/buildings/" + entityId;

        List<PlanStep> steps = List.of(
            new PlanStep(
                "step-1", planId, wfPlanStep, "new",
                building.id(), listUrl, 0,
                "Открываю карточку здания",
                List.of(new PlanStepAction(actOpen.id(), listUrl))
            ),
            new PlanStep(
                "step-2", planId, wfPlanStep, "new",
                building.id(), null, 1,
                "Навожу курсор на действие «Заказать выписку ЕГРН»",
                List.of(new PlanStepAction(actExplain.id(), null))
            ),
            new PlanStep(
                "step-3", planId, wfPlanStep, "new",
                building.id(), "action(" + actClick.id() + ")", 2,
                "Подсвечиваю действие",
                List.of(new PlanStepAction(actHover.id(), null))
            ),
            new PlanStep(
                "step-4", planId, wfPlanStep, "new",
                building.id(), "action(" + actClick.id() + ")", 3,
                "Запускаю заказ выписки",
                List.of(new PlanStepAction(actClick.id(), null))
            ),
            new PlanStep(
                "step-5", planId, wfPlanStep, "new",
                building.id(), "result", 4,
                "Жду результата выполнения действия",
                List.of(new PlanStepAction(actWait.id(), "5000"))
            )
        );

        Plan plan = new Plan(
            planId,
            "wf-plan",
            "in_progress",
            "step-1",
            "Здание " + entityId,
            "Заказ выписки ЕГРН",
            steps
        );

        String agentBaseUrl = "http://localhost:3000";
        AgentClient agentClient = new AgentClient(agentBaseUrl);

        String appBaseUrl = "http://localhost:8080";
        boolean headless = false;
        AgentService agentService = new AgentService(agentClient, resolver, appBaseUrl, headless);

        PlanExecutor executor = new PlanExecutor(agentService);
        PlanExecutionResult result = executor.execute(plan);

        System.out.println("=== Plan execution finished ===");
        System.out.println("Plan id: " + result.planId());
        System.out.println("Success: " + result.success());
        result.logEntries().forEach(entry -> {
            System.out.printf(
                "[%s] #%d %s -> success=%s, msg=%s, err=%s%n",
                entry.loggedAt(),
                entry.stepIndex(),
                entry.step().displayName(),
                entry.result().success(),
                entry.result().message(),
                entry.result().error()
            );
        });
    }
}
