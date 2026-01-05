package org.example.api.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.example.api.dto.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        webRequest = new ServletWebRequest(servletRequest);
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("request", "entityType", "Entity type is required"));
        fieldErrors.add(new FieldError("request", "entityId", "Entity ID is required"));

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        // When
        ResponseEntity<ErrorResponseDTO> response = handler.handleValidationExceptions(ex, webRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Validation Failed", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Entity type is required"));
        assertTrue(response.getBody().getMessage().contains("Entity ID is required"));
    }

    @Test
    void shouldHandleConstraintViolationException() {
        // Given
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        when(violation1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation1.getPropertyPath().toString()).thenReturn("entityType");
        when(violation1.getMessage()).thenReturn("must not be blank");
        
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        when(violation2.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation2.getPropertyPath().toString()).thenReturn("action");
        when(violation2.getMessage()).thenReturn("must not be null");
        
        violations.add(violation1);
        violations.add(violation2);
        
        when(ex.getConstraintViolations()).thenReturn(violations);

        // When
        ResponseEntity<ErrorResponseDTO> response = handler.handleConstraintViolationException(ex, webRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Validation Failed", response.getBody().getError());
        assertNotNull(response.getBody().getMessage());
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("EntityType not found: Building");

        // When
        ResponseEntity<ErrorResponseDTO> response = handler.handleIllegalArgumentException(ex, webRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Invalid Request", response.getBody().getError());
        assertEquals("EntityType not found: Building", response.getBody().getMessage());
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        RuntimeException ex = new RuntimeException("Unexpected error occurred");

        // When
        ResponseEntity<ErrorResponseDTO> response = handler.handleGenericException(ex, webRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Unexpected error occurred"));
    }

    @Test
    void shouldSetTimestampInErrorResponse() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Test error");

        // When
        ResponseEntity<ErrorResponseDTO> response = handler.handleIllegalArgumentException(ex, webRequest);

        // Then
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTimestamp());
    }
}

