package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.ActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionRepository extends JpaRepository<ActionEntity, String> {

    /** Действия, применимые к данному типу сущности (action_applicable_entity_type). */
    List<ActionEntity> findByApplicableEntityTypes_Id(String entityTypeId);
}
