package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.WorkflowStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStepEntity, String> {

    Optional<WorkflowStepEntity> findByInternalname(String internalname);
}
