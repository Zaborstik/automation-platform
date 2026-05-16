package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic plan generator that produces a deterministic plan from raw user input.
 *
 * <p>The stub avoids any external LLM dependency so that the microservice
 * remains usable without API keys. Action ids and entity-type ids match the
 * seed data inserted by Flyway in {@code V2__Insert_initial_data.sql}, so the
 * generated plan can be persisted by platform-api as-is.
 */
@Component
public class StubPlanGenerator {

    public static final String WORKFLOW_PLAN_ID = "wf-plan";
    public static final String WORKFLOW_PLAN_STEP_ID = "wf-plan-step";
    public static final String WORKFLOW_STEP_NEW = "new";

    public static final String ACTION_OPEN_PAGE = "act-open-page";
    public static final String ACTION_EXPLAIN = "act-explain";
    public static final String ACTION_CLICK = "act-click";

    public static final String ENTITY_TYPE_PAGE = "ent-page";
    public static final String ENTITY_TYPE_BUTTON = "ent-button";

    private static final Logger log = LoggerFactory.getLogger(StubPlanGenerator.class);

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[\\w./:?#&=+%-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICK_PATTERN = Pattern.compile(
        "(?i)(нажм|кликн|click|нажат|press)\\s+(?:на\\s+)?[\"'«]?([^\"'»]+?)[\"'»]?(?:\\s|$)"
    );

    public Plan generate(String userInput) {
        Objects.requireNonNull(userInput, "userInput cannot be null");
        String trimmed = userInput.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("userInput cannot be blank");
        }

        String planId = UUID.randomUUID().toString();
        List<PlanStep> steps = new ArrayList<>();
        int sortOrder = 0;

        String url = extractUrl(trimmed);
        if (url != null) {
            steps.add(buildStep(planId, sortOrder++, ENTITY_TYPE_PAGE, url,
                "Открыть страницу: " + url, ACTION_OPEN_PAGE, url));
        }

        String clickTarget = extractClickTarget(trimmed);
        if (clickTarget != null) {
            steps.add(buildStep(planId, sortOrder++, ENTITY_TYPE_BUTTON, clickTarget,
                "Кликнуть по: " + clickTarget, ACTION_CLICK, null));
        }

        steps.add(buildStep(planId, sortOrder, ENTITY_TYPE_PAGE, null,
            trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed,
            ACTION_EXPLAIN, null));

        Plan plan = new Plan(
            planId,
            WORKFLOW_PLAN_ID,
            WORKFLOW_STEP_NEW,
            steps.get(0).id(),
            url != null ? url : trimmed,
            trimmed,
            steps
        );

        log.debug("Stub generated plan {} with {} step(s) from input='{}'", planId, steps.size(), trimmed);
        return plan;
    }

    private static PlanStep buildStep(String planId, int sortOrder, String entityTypeId,
                                      String entityId, String displayName,
                                      String actionId, String metaValue) {
        return new PlanStep(
            UUID.randomUUID().toString(),
            planId,
            WORKFLOW_PLAN_STEP_ID,
            WORKFLOW_STEP_NEW,
            entityTypeId,
            entityId,
            sortOrder,
            displayName,
            List.of(new PlanStepAction(actionId, metaValue))
        );
    }

    private static String extractUrl(String text) {
        Matcher m = URL_PATTERN.matcher(text);
        if (!m.find()) {
            return null;
        }
        String candidate = m.group(1);
        try {
            URI uri = URI.create(candidate);
            return uri.toString();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String extractClickTarget(String text) {
        Matcher m = CLICK_PATTERN.matcher(text);
        return m.find() ? m.group(2).trim() : null;
    }
}
