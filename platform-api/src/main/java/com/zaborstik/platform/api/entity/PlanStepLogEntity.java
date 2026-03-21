package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Лог по шагу выполнения (zbrtstk.plan_step_log). Создаётся при падении/прерывании; может содержать скриншот (attachment).
 */
@Entity
@Table(name = "plan_step_log", schema = "zbrtstk")
public class PlanStepLogEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan", nullable = false)
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_step", nullable = false)
    private PlanStepEntity planStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_result", nullable = false)
    private PlanResultEntity planResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action", nullable = false)
    private ActionEntity action;

    @Column(name = "message", nullable = false, length = 510)
    private String message;

    @Column(name = "error", length = 2000)
    private String error;

    @Column(name = "executed_time", nullable = false)
    private Instant executedTime;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment")
    private AttachmentEntity attachment;

    public PlanStepLogEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public PlanEntity getPlan() { return plan; }
    public void setPlan(PlanEntity plan) { this.plan = plan; }
    public PlanStepEntity getPlanStep() { return planStep; }
    public void setPlanStep(PlanStepEntity planStep) { this.planStep = planStep; }
    public PlanResultEntity getPlanResult() { return planResult; }
    public void setPlanResult(PlanResultEntity planResult) { this.planResult = planResult; }
    public ActionEntity getAction() { return action; }
    public void setAction(ActionEntity action) { this.action = action; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Instant getExecutedTime() { return executedTime; }
    public void setExecutedTime(Instant executedTime) { this.executedTime = executedTime; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public AttachmentEntity getAttachment() { return attachment; }
    public void setAttachment(AttachmentEntity attachment) { this.attachment = attachment; }
}
