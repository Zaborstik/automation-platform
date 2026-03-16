package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntityTypeService {

    private final EntityTypeRepository entityTypeRepository;

    public EntityTypeService(EntityTypeRepository entityTypeRepository) {
        this.entityTypeRepository = entityTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<EntityTypeEntity> listAll() {
        return entityTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<EntityTypeEntity> getById(String id) {
        return entityTypeRepository.findById(id);
    }

    @Transactional
    public EntityTypeEntity create(String displayname, String uiDescription, String kmArticle) {
        EntityTypeEntity entity = new EntityTypeEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setDisplayname(displayname);
        entity.setUiDescription(uiDescription);
        entity.setKmArticle(kmArticle);
        return entityTypeRepository.save(entity);
    }

    @Transactional
    public EntityTypeEntity update(String id, String displayname, String uiDescription, String kmArticle) {
        EntityTypeEntity entity = entityTypeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Entity type not found: " + id));
        entity.setDisplayname(displayname);
        entity.setUiDescription(uiDescription);
        entity.setKmArticle(kmArticle);
        return entityTypeRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        entityTypeRepository.deleteById(id);
    }
}
