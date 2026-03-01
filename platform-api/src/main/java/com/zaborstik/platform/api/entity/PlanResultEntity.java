package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Итог выполнения плана (zbrtstk.plan_result). Создаётся после выполнения плана.
 */
@Entity
@Table(name = "plan_result", schema = "zbrtstk")
public class PlanResultEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan", nullable = false)
    private PlanEntity plan;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "started_time", nullable = false)
    private Instant startedTime;

    @Column(name = "finished_time", nullable = false)
    private Instant finishedTime;

    public PlanResultEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public PlanEntity getPlan() { return plan; }
    public void setPlan(PlanEntity plan) { this.plan = plan; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Instant getStartedTime() { return startedTime; }
    public void setStartedTime(Instant startedTime) { this.startedTime = startedTime; }
    public Instant getFinishedTime() { return finishedTime; }
    public void setFinishedTime(Instant finishedTime) { this.finishedTime = finishedTime; }
}
