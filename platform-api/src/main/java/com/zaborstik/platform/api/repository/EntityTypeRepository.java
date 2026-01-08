package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.EntityTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository для EntityTypeEntity.
 * 
 * Repository for EntityTypeEntity.
 */
@Repository
public interface EntityTypeRepository extends JpaRepository<EntityTypeEntity, String> {
}
