package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.PlanEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, String> {

    Page<PlanEntity> findByWorkflowStepInternalname(String status, Pageable pageable);

    Page<PlanEntity> findAll(Pageable pageable);
}
