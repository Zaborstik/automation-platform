package com.zaborstik.platform.knowledge.controller;

import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.knowledge.dto.GeneratePlanRequest;
import com.zaborstik.platform.knowledge.service.StubPlanGenerator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point of the knowledge microservice.
 *
 * <p>The platform-api server calls {@code POST /api/knowledge/generate-plan}
 * with a free-form user input and expects back a fully populated
 * {@link Plan} that can be persisted as-is. No state is kept here;
 * every call is independent.
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final StubPlanGenerator planGenerator;

    public KnowledgeController(StubPlanGenerator planGenerator) {
        this.planGenerator = planGenerator;
    }

    @PostMapping("/generate-plan")
    public ResponseEntity<Plan> generatePlan(@Valid @RequestBody GeneratePlanRequest request) {
        Plan plan = planGenerator.generate(request.getUserInput());
        return ResponseEntity.ok(plan);
    }
}
