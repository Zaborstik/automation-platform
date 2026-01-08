package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository для PlanEntity.
 * 
 * Repository for PlanEntity.
 */
@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, String> {
}
