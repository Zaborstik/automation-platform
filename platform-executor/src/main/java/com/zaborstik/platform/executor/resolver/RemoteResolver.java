package com.zaborstik.platform.executor.resolver;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.ActionType;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.domain.Workflow;
import com.zaborstik.platform.core.domain.WorkflowStep;
import com.zaborstik.platform.core.domain.WorkflowTransition;
import com.zaborstik.platform.core.resolver.Resolver;
import com.zaborstik.platform.executor.client.RemoteApiClient;
import com.zaborstik.platform.executor.client.api.ActionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * {@link Resolver} implementation that fetches data from the remote
 * {@code platform-api} via HTTP and caches it for the duration of a single
 * plan run.
 *
 * <p>Only the methods actually exercised by the local executor and by the
 * agent's step-to-command translator are implemented; the rest return empty
 * results. The server-side {@code DatabaseResolver} is the authoritative
 * implementation.
 */
@Component
public class RemoteResolver implements Resolver {

    private static final Logger log = LoggerFactory.getLogger(RemoteResolver.class);

    private final RemoteApiClient apiClient;

    private final Map<String, Optional<Action>> actionCache = new ConcurrentHashMap<>();
    private final Map<String, List<Action>> actionsByEntityType = new ConcurrentHashMap<>();

    public RemoteResolver(RemoteApiClient apiClient) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
    }

    public void resetCache() {
        actionCache.clear();
        actionsByEntityType.clear();
    }

    @Override
    public Optional<EntityType> findEntityType(String entityTypeId) {
        return Optional.empty();
    }

    @Override
    public Optional<Action> findAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return Optional.empty();
        }
        return actionCache.computeIfAbsent(actionId, this::fetchAction);
    }

    private Optional<Action> fetchAction(String actionId) {
        try {
            return apiClient.fetchAction(actionId).map(RemoteResolver::toAction);
        } catch (Exception ex) {
            log.warn("Action {} could not be fetched: {}", actionId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<ActionType> findActionType(String actionTypeId) {
        return Optional.empty();
    }

    @Override
    public Optional<Workflow> findWorkflow(String workflowId) {
        return Optional.empty();
    }

    @Override
    public Optional<WorkflowStep> findWorkflowStep(String workflowStepId) {
        return Optional.empty();
    }

    @Override
    public Optional<WorkflowStep> findWorkflowStepByInternalName(String internalName) {
        return Optional.empty();
    }

    @Override
    public List<WorkflowTransition> findTransitions(String workflowId) {
        return List.of();
    }

    @Override
    public Optional<WorkflowTransition> findTransition(String workflowId, String fromStep, String toStep) {
        return Optional.empty();
    }

    @Override
    public List<Action> findActionsApplicableToEntityType(String entityTypeId) {
        if (entityTypeId == null || entityTypeId.isBlank()) {
            return List.of();
        }
        return actionsByEntityType.computeIfAbsent(entityTypeId, this::fetchActionsByEntityType);
    }

    private List<Action> fetchActionsByEntityType(String entityTypeId) {
        try {
            List<ActionDto> dtos = apiClient.fetchActionsByEntityType(entityTypeId);
            return dtos.stream().map(RemoteResolver::toAction).collect(Collectors.toUnmodifiableList());
        } catch (Exception ex) {
            log.warn("Actions for entityType {} could not be fetched: {}", entityTypeId, ex.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean isActionApplicable(String actionId, String entityTypeId) {
        return findActionsApplicableToEntityType(entityTypeId).stream()
            .anyMatch(a -> a.id().equals(actionId));
    }

    @Override
    public Optional<UIBinding> findUIBinding(String actionId) {
        // UI bindings are not exposed by platform-api today; rely on selectors
        // baked into PlanStep#entityId (CSS/XPath supplied by the planner).
        return Optional.empty();
    }

    @Override
    public boolean isWorkflowStepInternalName(String internalName) {
        // The executor does not need workflow-step lifecycle awareness; the
        // server is the source of truth for lifecycle transitions.
        if (internalName == null) {
            return false;
        }
        return switch (internalName) {
            case "new", "in_progress", "paused", "completed", "failed", "cancelled" -> true;
            default -> false;
        };
    }

    /**
     * Returns a {@code Map<actionId, selector>} for the actions referenced in
     * {@code actionIds}. Only {@code action(<id>)}-style entity ids need this,
     * and platform-api currently exposes no UI bindings, so the map is empty
     * by default. Adding bindings in the future is a single API addition.
     */
    public Map<String, String> resolveSelectors(List<String> actionIds) {
        return new HashMap<>();
    }

    private static Action toAction(ActionDto dto) {
        Instant now = Instant.now();
        return new Action(
            dto.id,
            dto.displayname != null ? dto.displayname : dto.id,
            dto.internalname != null ? dto.internalname : dto.id,
            dto.metaValue,
            dto.description,
            dto.actionTypeId != null ? dto.actionTypeId : dto.id,
            now,
            now
        );
    }
}
