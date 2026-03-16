package com.zaborstik.platform.api.resolver;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.ActionTypeEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.ActionTypeRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.entity")
@Import(DatabaseResolver.class)
@ActiveProfiles({"dev", "datajpa"})
class DatabaseResolverTest {

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private ActionTypeRepository actionTypeRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private DatabaseResolver databaseResolver;

    @BeforeEach
    void setUp() {
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();
        actionTypeRepository.deleteAll();

        EntityTypeEntity et = new EntityTypeEntity();
        et.setId("Building");
        et.setDisplayname("Здание");
        entityTypeRepository.save(et);

        ActionTypeEntity actionType = new ActionTypeEntity();
        actionType.setId("act-type-1");
        actionType.setInternalname("interaction");
        actionType.setDisplayname("Взаимодействие");
        actionTypeRepository.save(actionType);

        ActionEntity action = new ActionEntity();
        action.setId("order_egrn_extract");
        action.setDisplayname("Заказать выписку из ЕГРН");
        action.setInternalname("order_egrn_extract");
        action.setDescription("Описание действия");
        action.setActionType(actionType);
        action.setApplicableEntityTypes(Set.of(et));
        actionRepository.save(action);
    }

    @Test
    void shouldFindEntityType() {
        Optional<EntityType> result = databaseResolver.findEntityType("Building");
        assertTrue(result.isPresent());
        assertEquals("Building", result.get().id());
        assertEquals("Здание", result.get().displayName());
    }

    @Test
    void shouldReturnEmptyWhenEntityTypeNotFound() {
        assertFalse(databaseResolver.findEntityType("NonExistent").isPresent());
    }

    @Test
    void shouldFindAction() {
        Optional<Action> result = databaseResolver.findAction("order_egrn_extract");
        assertTrue(result.isPresent());
        assertEquals("order_egrn_extract", result.get().id());
        assertTrue(databaseResolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(databaseResolver.isActionApplicable("order_egrn_extract", "Contract"));
    }

    @Test
    void shouldReturnEmptyWhenActionNotFound() {
        assertFalse(databaseResolver.findAction("non_existent").isPresent());
    }

    @Test
    void shouldReturnEmptyForUIBinding() {
        assertFalse(databaseResolver.findUIBinding("order_egrn_extract").isPresent());
    }

    @Test
    void shouldCheckActionApplicability() {
        assertTrue(databaseResolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(databaseResolver.isActionApplicable("order_egrn_extract", "Contract"));
    }
}
