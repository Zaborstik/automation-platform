package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.UIBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UIBindingRepository extends JpaRepository<UIBindingEntity, String> {

    Optional<UIBindingEntity> findByAction(String actionId);
}
