package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.ActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository для ActionEntity.
 * 
 * Repository for ActionEntity.
 */
@Repository
public interface ActionRepository extends JpaRepository<ActionEntity, String> {
}
