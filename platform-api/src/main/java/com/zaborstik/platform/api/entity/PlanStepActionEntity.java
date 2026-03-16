package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.io.Serial;

/**
 * Связка шаг плана — действие (zbrtstk.plan_step_action). У одного plan_step может быть несколько action; meta_value — доп. инфо (например текст для поиска).
 */
@Entity
@Table(name = "plan_step_action", schema = "zbrtstk")
@IdClass(PlanStepActionEntity.PlanStepActionId.class)
public class PlanStepActionEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_step", nullable = false)
    private PlanStepEntity planStep;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action", nullable = false)
    private ActionEntity action;

    @Column(name = "meta_value", columnDefinition = "TEXT")
    private String metaValue;

    public static class PlanStepActionId implements java.io.Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String planStep;
        private String action;
        public PlanStepActionId() {}
        public PlanStepActionId(String planStep, String action) {
            this.planStep = planStep;
            this.action = action;
        }
        public String getPlanStep() { return planStep; }
        public void setPlanStep(String planStep) { this.planStep = planStep; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PlanStepActionId that)) return false;
            return java.util.Objects.equals(planStep, that.planStep) && java.util.Objects.equals(action, that.action);
        }
        @Override
        public int hashCode() {
            return java.util.Objects.hash(planStep, action);
        }
    }

    public PlanStepActionEntity() {}

    public PlanStepEntity getPlanStep() { return planStep; }
    public void setPlanStep(PlanStepEntity planStep) { this.planStep = planStep; }
    public ActionEntity getAction() { return action; }
    public void setAction(ActionEntity action) { this.action = action; }
    public String getMetaValue() { return metaValue; }
    public void setMetaValue(String metaValue) { this.metaValue = metaValue; }
}
