package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.PlanStepLogEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanStepLogEntryRepository extends JpaRepository<PlanStepLogEntryEntity, String> {

    List<PlanStepLogEntryEntity> findByPlan_Id(String planId);
}
