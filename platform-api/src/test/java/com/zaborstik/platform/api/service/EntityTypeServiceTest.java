package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.entity.EntityTypeEntity;
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
class EntityTypeServiceTest {

    @Mock
    private EntityTypeRepository entityTypeRepository;

    @InjectMocks
    private EntityTypeService entityTypeService;

    @Test
    void listAllShouldReturnValues() {
        when(entityTypeRepository.findAll()).thenReturn(List.of(new EntityTypeEntity(), new EntityTypeEntity()));
        assertEquals(2, entityTypeService.listAll().size());
    }

    @Test
    void getByIdShouldReturnFoundValue() {
        EntityTypeEntity entity = new EntityTypeEntity();
        entity.setId("ent-1");
        when(entityTypeRepository.findById("ent-1")).thenReturn(Optional.of(entity));

        Optional<EntityTypeEntity> result = entityTypeService.getById("ent-1");

        assertTrue(result.isPresent());
        assertEquals("ent-1", result.get().getId());
    }

    @Test
    void createShouldGenerateIdAndSaveEntity() {
        when(entityTypeRepository.save(any(EntityTypeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EntityTypeEntity result = entityTypeService.create("Form", "ui", "km-1");

        assertNotNull(result.getId());
        assertEquals("Form", result.getDisplayname());
        assertEquals("ui", result.getUiDescription());
        assertEquals("km-1", result.getKmArticle());
    }

    @Test
    void updateShouldPersistChanges() {
        EntityTypeEntity entity = new EntityTypeEntity();
        entity.setId("ent-1");
        when(entityTypeRepository.findById("ent-1")).thenReturn(Optional.of(entity));
        when(entityTypeRepository.save(any(EntityTypeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EntityTypeEntity updated = entityTypeService.update("ent-1", "Updated", "ui2", "km-2");

        assertEquals("Updated", updated.getDisplayname());
        assertEquals("ui2", updated.getUiDescription());
        assertEquals("km-2", updated.getKmArticle());
    }

    @Test
    void updateShouldThrowWhenMissing() {
        when(entityTypeRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> entityTypeService.update("missing", "a", "b", "c"));
    }

    @Test
    void deleteShouldDelegateToRepository() {
        entityTypeService.delete("ent-1");
        verify(entityTypeRepository).deleteById("ent-1");
    }
}
