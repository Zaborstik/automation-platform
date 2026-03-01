package com.zaborstik.platform.core.example;

import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.ActionType;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.domain.Workflow;
import com.zaborstik.platform.core.domain.WorkflowStep;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.resolver.InMemoryResolver;

import java.util.Map;

/**
 * Пример использования под новую модель БД:
 * action_type → action, action_applicable_entity_type, plan → plan_step → plan_step_action.
 */
public class ExampleUsage {
    public static void main(String[] args) {
        InMemoryResolver resolver = new InMemoryResolver();

        // Workflow и шаги ЖЦ (как в V2)
        resolver.registerWorkflowStep(new WorkflowStep("wfs-new", "new", "Новая", 10));
        resolver.registerWorkflowStep(new WorkflowStep("wfs-in-progress", "in_progress", "В работе", 20));
        resolver.registerWorkflow(new Workflow("wf-plan", "Жизненный цикл плана", "wfs-new"));
        resolver.registerWorkflow(new Workflow("wf-plan-step", "Жизненный цикл шага плана", "wfs-new"));

        // Тип действия и действие
        resolver.registerActionType(new ActionType("act-type-interaction", "interaction", "Взаимодействие с UI"));
        Action action = Action.of("act-click", "Клик по элементу", "click", "Нажатие на элемент.", "act-type-interaction");
        resolver.registerAction(action);

        // Тип сущности и применимость (action_applicable_entity_type)
        resolver.registerEntityType(EntityType.of("ent-button", "Кнопка"));
        resolver.registerActionApplicableToEntityType("act-click", "ent-button");

        // Опционально: UI binding для executor
        resolver.registerUIBinding(new UIBinding("act-click", "[data-action='submit']", UIBinding.SelectorType.CSS, Map.of()));

        ExecutionEngine engine = new ExecutionEngine(resolver);
        ExecutionRequest request = new ExecutionRequest(
            "ent-button",
            "btn-1",
            "act-click",
            Map.of("meta_value", "optional search text")
        );

        Plan plan = engine.createPlan(request);
        System.out.println("План: " + plan.id());
        System.out.println("  workflow=" + plan.workflowId() + ", step=" + plan.workflowStepInternalName());
        System.out.println("  stoppedAtPlanStepId=" + plan.stoppedAtPlanStepId());
        System.out.println("  explanation=" + plan.explanation());
        plan.steps().forEach(step -> {
            System.out.println("  Шаг: " + step.displayName());
            step.actions().forEach(a -> System.out.println("    action=" + a.actionId() + ", metaValue=" + a.metaValue()));
        });
    }
}
