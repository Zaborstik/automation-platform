package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.WorkflowTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransitionEntity, String> {

    List<WorkflowTransitionEntity> findByWorkflow_Id(String workflowId);

    Optional<WorkflowTransitionEntity> findByWorkflow_IdAndFromStepAndToStep(String workflowId, String fromStep, String toStep);
}
