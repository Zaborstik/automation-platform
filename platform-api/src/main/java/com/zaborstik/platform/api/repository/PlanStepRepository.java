package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.PlanStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanStepRepository extends JpaRepository<PlanStepEntity, String> {

    List<PlanStepEntity> findByPlan_IdOrderBySortorder(String planId);

    Optional<PlanStepEntity> findByIdAndPlan_Id(String stepId, String planId);
}
