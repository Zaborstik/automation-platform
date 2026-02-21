package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.dto.EntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Единый репозиторий для таблицы {@code entities}.
 */
@Repository
public interface EntityRepository extends JpaRepository<EntityDTO, EntityId> {

    List<EntityDTO> findById_TableName(String tableName);

    default java.util.Optional<EntityDTO> findByTableNameAndId(String tableName, String id) {
        return findById(new EntityId(tableName, id));
    }
}
