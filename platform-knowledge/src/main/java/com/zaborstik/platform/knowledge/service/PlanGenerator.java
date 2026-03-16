package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;
import com.zaborstik.platform.knowledge.model.ParsedUserRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PlanGenerator {

    public static final String WORKFLOW_PLAN_ID = "wf-plan";
    public static final String WORKFLOW_PLAN_STEP_ID = "wf-plan-step";
    public static final String WORKFLOW_STEP_NEW = "new";

    private final Resolver resolver;

    public PlanGenerator(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver cannot be null");
    }

    public Plan generate(ParsedUserRequest request) {
        String target = request.parameters().get("target");
        return generate(request, target, request.rawInput());
    }

    public Plan generate(ParsedUserRequest request, String target, String explanation) {
        Objects.requireNonNull(request, "request cannot be null");
        if (request.actionIds().isEmpty()) {
            throw new IllegalArgumentException("No actionIds provided");
        }
        if (request.entityTypeId() == null || request.entityTypeId().isBlank()) {
            throw new IllegalArgumentException("entityTypeId is required");
        }

        String planId = UUID.randomUUID().toString();
        List<PlanStep> steps = new ArrayList<>();
        int sortOrder = 1;

        for (String actionId : request.actionIds()) {
            if (!resolver.isActionApplicable(actionId, request.entityTypeId())) {
                continue;
            }

            String stepId = UUID.randomUUID().toString();
            String metaValue = request.parameters().get("meta_value");
            PlanStep step = new PlanStep(
                stepId,
                planId,
                WORKFLOW_PLAN_STEP_ID,
                WORKFLOW_STEP_NEW,
                request.entityTypeId(),
                request.parameters().get("entity_id"),
                sortOrder++,
                "Execute action: " + actionId,
                List.of(new PlanStepAction(actionId, metaValue))
            );
            steps.add(step);
        }

        if (steps.isEmpty()) {
            throw new IllegalArgumentException("No applicable actions for entityTypeId=" + request.entityTypeId());
        }

        return new Plan(
            planId,
            WORKFLOW_PLAN_ID,
            WORKFLOW_STEP_NEW,
            steps.get(0).id(),
            target,
            explanation,
            steps
        );
    }
}
