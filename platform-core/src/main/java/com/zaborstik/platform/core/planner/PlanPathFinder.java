package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Поиск последовательностей действий через граф применимости action -> entity_type.
 */
public class PlanPathFinder {

    private final Resolver resolver;

    public PlanPathFinder(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver cannot be null");
    }

    public List<Action> findApplicableActions(String entityTypeId) {
        Objects.requireNonNull(entityTypeId, "entityTypeId cannot be null");
        return resolver.findActionsApplicableToEntityType(entityTypeId);
    }

    public List<PlanStepAction> buildActionSequence(List<String> entityTypeIds, List<String> actionIds) {
        Objects.requireNonNull(entityTypeIds, "entityTypeIds cannot be null");
        Objects.requireNonNull(actionIds, "actionIds cannot be null");
        if (entityTypeIds.size() != actionIds.size()) {
            throw new IllegalArgumentException("entityTypeIds and actionIds must have same size");
        }

        List<PlanStepAction> sequence = new ArrayList<>(actionIds.size());
        for (int i = 0; i < actionIds.size(); i++) {
            String entityTypeId = Objects.requireNonNull(entityTypeIds.get(i), "entityTypeId cannot be null");
            String actionId = Objects.requireNonNull(actionIds.get(i), "actionId cannot be null");
            if (!canExecute(actionId, entityTypeId)) {
                throw new IllegalArgumentException(
                    "Action '" + actionId + "' is not applicable to entity type '" + entityTypeId + "'"
                );
            }
            sequence.add(new PlanStepAction(actionId, null));
        }
        return sequence;
    }

    public boolean canExecute(String actionId, String entityTypeId) {
        Objects.requireNonNull(actionId, "actionId cannot be null");
        Objects.requireNonNull(entityTypeId, "entityTypeId cannot be null");
        return resolver.isActionApplicable(actionId, entityTypeId);
    }
}
