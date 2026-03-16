package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.ActionTypeEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.ActionTypeRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final EntityTypeRepository entityTypeRepository;

    public ActionService(ActionRepository actionRepository,
                         ActionTypeRepository actionTypeRepository,
                         EntityTypeRepository entityTypeRepository) {
        this.actionRepository = actionRepository;
        this.actionTypeRepository = actionTypeRepository;
        this.entityTypeRepository = entityTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<ActionEntity> listAll() {
        return actionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ActionEntity> getById(String id) {
        return actionRepository.findById(id);
    }

    @Transactional
    public ActionEntity create(String displayname,
                               String internalname,
                               String description,
                               String actionTypeId,
                               String metaValue) {
        ActionTypeEntity actionType = actionTypeRepository.findById(actionTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Action type not found: " + actionTypeId));

        ActionEntity entity = new ActionEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setDisplayname(displayname);
        entity.setInternalname(internalname);
        entity.setDescription(description);
        entity.setActionType(actionType);
        entity.setMetaValue(metaValue);
        return actionRepository.save(entity);
    }

    @Transactional
    public ActionEntity update(String id, String displayname, String description, String metaValue) {
        ActionEntity entity = actionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Action not found: " + id));
        entity.setDisplayname(displayname);
        entity.setDescription(description);
        entity.setMetaValue(metaValue);
        return actionRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        actionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ActionEntity> findByEntityType(String entityTypeId) {
        if (!entityTypeRepository.existsById(entityTypeId)) {
            throw new IllegalArgumentException("Entity type not found: " + entityTypeId);
        }
        return actionRepository.findByApplicableEntityTypes_Id(entityTypeId);
    }
}
