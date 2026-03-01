package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {
}
