package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.ActionTypeEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.ActionTypeRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

    @Mock
    private ActionRepository actionRepository;

    @Mock
    private ActionTypeRepository actionTypeRepository;

    @Mock
    private EntityTypeRepository entityTypeRepository;

    @InjectMocks
    private ActionService actionService;

    @Test
    void listAllShouldReturnActions() {
        when(actionRepository.findAll()).thenReturn(List.of(new ActionEntity(), new ActionEntity()));

        List<ActionEntity> result = actionService.listAll();

        assertEquals(2, result.size());
        verify(actionRepository).findAll();
    }

    @Test
    void getByIdShouldReturnActionWhenPresent() {
        ActionEntity action = new ActionEntity();
        action.setId("act-1");
        when(actionRepository.findById("act-1")).thenReturn(Optional.of(action));

        Optional<ActionEntity> result = actionService.getById("act-1");

        assertTrue(result.isPresent());
        assertEquals("act-1", result.get().getId());
    }

    @Test
    void getByIdShouldReturnEmptyWhenMissing() {
        when(actionRepository.findById("missing")).thenReturn(Optional.empty());
        assertTrue(actionService.getById("missing").isEmpty());
    }

    @Test
    void createShouldSaveActionWhenActionTypeExists() {
        ActionTypeEntity actionType = new ActionTypeEntity();
        actionType.setId("type-1");
        when(actionTypeRepository.findById("type-1")).thenReturn(Optional.of(actionType));
        when(actionRepository.save(any(ActionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActionEntity created = actionService.create("Click", "click", "desc", "type-1", "meta");

        assertNotNull(created.getId());
        assertEquals("Click", created.getDisplayname());
        assertEquals("click", created.getInternalname());
        assertEquals("type-1", created.getActionType().getId());
        verify(actionRepository).save(any(ActionEntity.class));
    }

    @Test
    void createShouldThrowWhenActionTypeMissing() {
        when(actionTypeRepository.findById("missing")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> actionService.create("Click", "click", "desc", "missing", "meta")
        );

        assertEquals("Action type not found: missing", ex.getMessage());
    }

    @Test
    void updateShouldPersistChangesWhenActionExists() {
        ActionEntity action = new ActionEntity();
        action.setId("act-1");
        action.setDisplayname("Old");
        when(actionRepository.findById("act-1")).thenReturn(Optional.of(action));
        when(actionRepository.save(any(ActionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActionEntity updated = actionService.update("act-1", "New", "new-desc", "new-meta");

        assertEquals("New", updated.getDisplayname());
        assertEquals("new-desc", updated.getDescription());
        assertEquals("new-meta", updated.getMetaValue());
        verify(actionRepository).save(action);
    }

    @Test
    void updateShouldThrowWhenActionMissing() {
        when(actionRepository.findById("act-404")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> actionService.update("act-404", "x", "y", "z")
        );

        assertEquals("Action not found: act-404", ex.getMessage());
    }

    @Test
    void deleteShouldDelegateToRepository() {
        actionService.delete("act-delete");
        verify(actionRepository).deleteById("act-delete");
    }

    @Test
    void findByEntityTypeShouldDelegateToRepository() {
        when(entityTypeRepository.existsById("ent-1")).thenReturn(true);
        when(actionRepository.findByApplicableEntityTypes_Id("ent-1")).thenReturn(List.of(new ActionEntity()));

        List<ActionEntity> result = actionService.findByEntityType("ent-1");

        assertEquals(1, result.size());
        verify(actionRepository).findByApplicableEntityTypes_Id("ent-1");
    }
}
