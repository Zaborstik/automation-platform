package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.ExecutionResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository для ExecutionResultEntity.
 * 
 * Repository for ExecutionResultEntity.
 */
@Repository
public interface ExecutionResultRepository extends JpaRepository<ExecutionResultEntity, Long> {
    Optional<ExecutionResultEntity> findByPlanId(String planId);
}
