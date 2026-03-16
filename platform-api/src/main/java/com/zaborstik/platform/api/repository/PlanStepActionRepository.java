package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.PlanStepActionEntity;
import com.zaborstik.platform.api.entity.PlanStepActionEntity.PlanStepActionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanStepActionRepository extends JpaRepository<PlanStepActionEntity, PlanStepActionId> {

    List<PlanStepActionEntity> findByPlanStep_Id(String planStepId);
}
