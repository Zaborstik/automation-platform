package com.zaborstik.platform.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.api.dto.CreateActionRequest;
import com.zaborstik.platform.api.dto.UpdateActionRequest;
import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.ActionTypeEntity;
import com.zaborstik.platform.api.exception.GlobalExceptionHandler;
import com.zaborstik.platform.api.service.ActionService;
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

@WebMvcTest(ActionController.class)
@Import(GlobalExceptionHandler.class)
class ActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActionService actionService;

    @Test
    void shouldGetAllActions() throws Exception {
        when(actionService.listAll()).thenReturn(List.of(action("act-1", "click", "type-1")));

        mockMvc.perform(get("/api/actions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("act-1"))
            .andExpect(jsonPath("$[0].internalname").value("click"));
    }

    @Test
    void shouldGetActionById() throws Exception {
        when(actionService.getById("act-1")).thenReturn(Optional.of(action("act-1", "click", "type-1")));

        mockMvc.perform(get("/api/actions/act-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("act-1"));
    }

    @Test
    void shouldReturnNotFoundWhenActionMissing() throws Exception {
        when(actionService.getById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/actions/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Action with id 'missing' not found"));
    }

    @Test
    void shouldCreateAction() throws Exception {
        CreateActionRequest request = new CreateActionRequest();
        request.setDisplayname("Click");
        request.setInternalname("click");
        request.setDescription("desc");
        request.setActionTypeId("type-1");
        request.setMetaValue("meta");

        when(actionService.create(eq("Click"), eq("click"), eq("desc"), eq("type-1"), eq("meta")))
            .thenReturn(action("act-1", "click", "type-1"));

        mockMvc.perform(post("/api/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("act-1"));
    }

    @Test
    void shouldUpdateAction() throws Exception {
        UpdateActionRequest request = new UpdateActionRequest();
        request.setDisplayname("Click updated");
        request.setDescription("updated");
        request.setMetaValue("meta2");

        ActionEntity updated = action("act-1", "click", "type-1");
        updated.setDisplayname("Click updated");
        updated.setDescription("updated");
        updated.setMetaValue("meta2");
        when(actionService.update(eq("act-1"), eq("Click updated"), eq("updated"), eq("meta2"))).thenReturn(updated);

        mockMvc.perform(put("/api/actions/act-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayname").value("Click updated"));
    }

    @Test
    void shouldReturnNotFoundOnUpdateWhenActionMissing() throws Exception {
        UpdateActionRequest request = new UpdateActionRequest();
        request.setDisplayname("x");

        when(actionService.update(eq("missing"), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Action not found: missing"));

        mockMvc.perform(put("/api/actions/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Action with id 'missing' not found"));
    }

    @Test
    void shouldDeleteAction() throws Exception {
        doNothing().when(actionService).delete("act-1");

        mockMvc.perform(delete("/api/actions/act-1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnBadRequestForInvalidPostBody() throws Exception {
        CreateActionRequest request = new CreateActionRequest();
        request.setDisplayname("");
        request.setInternalname("");
        request.setActionTypeId("");

        mockMvc.perform(post("/api/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    private static ActionEntity action(String id, String internalName, String actionTypeId) {
        ActionTypeEntity type = new ActionTypeEntity();
        type.setId(actionTypeId);
        ActionEntity action = new ActionEntity();
        action.setId(id);
        action.setDisplayname("Action " + id);
        action.setInternalname(internalName);
        action.setActionType(type);
        return action;
    }
}
