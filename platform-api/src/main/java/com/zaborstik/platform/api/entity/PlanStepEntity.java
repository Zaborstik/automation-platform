package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Entity для PlanStep.
 * Хранит шаги плана выполнения.
 */
@Entity
@Table(name = "plan_steps")
public class PlanStepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    @Column(name = "step_type", nullable = false, length = 50)
    private String type;

    @Column(name = "target", length = 1000)
    private String target;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    @ElementCollection
    @CollectionTable(name = "plan_step_parameters", joinColumns = @JoinColumn(name = "step_pk"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> parameters = new HashMap<>();

    @Column(name = "created_at")
    private java.time.Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.Instant.now();
    }

    // Constructors
    public PlanStepEntity() {
    }

    public PlanStepEntity(PlanEntity plan, Integer stepIndex, String type, String target, 
                          String explanation, Map<String, String> parameters) {
        this.plan = plan;
        this.stepIndex = stepIndex;
        this.type = type;
        this.target = target;
        this.explanation = explanation;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    // Getters and Setters
    public Long getPk() {
        return pk;
    }

    public void setPk(Long pk) {
        this.pk = pk;
    }

    public PlanEntity getPlan() {
        return plan;
    }

    public void setPlan(PlanEntity plan) {
        this.plan = plan;
    }

    public Integer getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(Integer stepIndex) {
        this.stepIndex = stepIndex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }
}
