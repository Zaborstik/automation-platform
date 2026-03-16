package com.zaborstik.platform.api.controller;

import com.zaborstik.platform.api.dto.CreateEntityTypeRequest;
import com.zaborstik.platform.api.dto.EntityTypeResponse;
import com.zaborstik.platform.api.dto.ErrorResponseDTO;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.service.EntityTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entity-types")
@Tag(name = "Entity Types")
public class EntityTypeController {

    private final EntityTypeService entityTypeService;

    public EntityTypeController(EntityTypeService entityTypeService) {
        this.entityTypeService = entityTypeService;
    }

    @GetMapping
    @Operation(summary = "List entity types")
    @ApiResponse(responseCode = "200", description = "Entity types returned")
    public ResponseEntity<List<EntityTypeResponse>> list() {
        return ResponseEntity.ok(entityTypeService.listAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get entity type by id")
    @ApiResponse(responseCode = "200", description = "Entity type found")
    @ApiResponse(responseCode = "404", description = "Entity type not found")
    public ResponseEntity<?> getById(@PathVariable("id") String id) {
        return entityTypeService.getById(id)
            .<ResponseEntity<?>>map(entity -> ResponseEntity.ok(toResponse(entity)))
            .orElseGet(() -> notFound(id));
    }

    @PostMapping
    @Operation(summary = "Create entity type")
    @ApiResponse(responseCode = "201", description = "Entity type created")
    public ResponseEntity<EntityTypeResponse> create(@Valid @RequestBody CreateEntityTypeRequest request) {
        EntityTypeEntity created = entityTypeService.create(
            request.getDisplayname(),
            request.getUiDescription(),
            request.getKmArticle()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update entity type")
    @ApiResponse(responseCode = "200", description = "Entity type updated")
    @ApiResponse(responseCode = "404", description = "Entity type not found")
    public ResponseEntity<?> update(@PathVariable("id") String id, @RequestBody CreateEntityTypeRequest request) {
        try {
            EntityTypeEntity updated = entityTypeService.update(
                id,
                request.getDisplayname(),
                request.getUiDescription(),
                request.getKmArticle()
            );
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Entity type not found:")) {
                return notFound(id);
            }
            throw ex;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete entity type")
    @ApiResponse(responseCode = "204", description = "Entity type deleted")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        entityTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ErrorResponseDTO> notFound(String id) {
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            "Entity type with id '" + id + "' not found",
            "/api/entity-types/" + id
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    private EntityTypeResponse toResponse(EntityTypeEntity entity) {
        EntityTypeResponse response = new EntityTypeResponse();
        response.setId(entity.getId());
        response.setDisplayname(entity.getDisplayname());
        response.setUiDescription(entity.getUiDescription());
        response.setKmArticle(entity.getKmArticle());
        response.setEntityfieldlist(entity.getEntityfieldlist());
        response.setButtons(entity.getButtons());
        response.setCreatedTime(entity.getCreatedTime());
        response.setUpdatedTime(entity.getUpdatedTime());
        return response;
    }
}
