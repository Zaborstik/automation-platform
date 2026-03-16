package com.zaborstik.platform.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.api.dto.CreateEntityTypeRequest;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.exception.GlobalExceptionHandler;
import com.zaborstik.platform.api.service.EntityTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EntityTypeController.class)
@Import(GlobalExceptionHandler.class)
class EntityTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EntityTypeService entityTypeService;

    @Test
    void shouldListEntityTypes() throws Exception {
        when(entityTypeService.listAll()).thenReturn(List.of(entity("ent-1", "Page")));

        mockMvc.perform(get("/api/entity-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("ent-1"))
            .andExpect(jsonPath("$[0].displayname").value("Page"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(entityTypeService.getById("ent-1")).thenReturn(Optional.of(entity("ent-1", "Page")));

        mockMvc.perform(get("/api/entity-types/ent-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("ent-1"));
    }

    @Test
    void shouldReturn404WhenMissing() throws Exception {
        when(entityTypeService.getById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/entity-types/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Entity type with id 'missing' not found"));
    }

    @Test
    void shouldCreateEntityType() throws Exception {
        CreateEntityTypeRequest request = new CreateEntityTypeRequest();
        request.setDisplayname("Form");
        request.setUiDescription("Form ui");
        request.setKmArticle("km-1");

        when(entityTypeService.create(eq("Form"), eq("Form ui"), eq("km-1"))).thenReturn(entity("ent-1", "Form"));

        mockMvc.perform(post("/api/entity-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("ent-1"));
    }

    @Test
    void shouldUpdateEntityType() throws Exception {
        CreateEntityTypeRequest request = new CreateEntityTypeRequest();
        request.setDisplayname("Updated");
        request.setUiDescription("Updated ui");
        request.setKmArticle("km-2");

        EntityTypeEntity updated = entity("ent-1", "Updated");
        updated.setUiDescription("Updated ui");
        updated.setKmArticle("km-2");
        when(entityTypeService.update(eq("ent-1"), eq("Updated"), eq("Updated ui"), eq("km-2"))).thenReturn(updated);

        mockMvc.perform(put("/api/entity-types/ent-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayname").value("Updated"));
    }

    @Test
    void shouldReturn404OnUpdateWhenMissing() throws Exception {
        CreateEntityTypeRequest request = new CreateEntityTypeRequest();
        request.setDisplayname("Updated");

        when(entityTypeService.update(eq("missing"), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Entity type not found: missing"));

        mockMvc.perform(put("/api/entity-types/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Entity type with id 'missing' not found"));
    }

    @Test
    void shouldDeleteEntityType() throws Exception {
        doNothing().when(entityTypeService).delete("ent-1");
        mockMvc.perform(delete("/api/entity-types/ent-1"))
            .andExpect(status().isNoContent());
    }

    private static EntityTypeEntity entity(String id, String displayName) {
        EntityTypeEntity entity = new EntityTypeEntity();
        entity.setId(id);
        entity.setDisplayname(displayName);
        return entity;
    }
}
