package com.paddykim.platform.cicd.execution;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cicd/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<ExecutionResponse> createExecution(@Valid @RequestBody ExecutionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(executionService.createExecution(request));
    }

    @GetMapping
    public List<ExecutionResponse> listExecutions() {
        return executionService.listExecutions();
    }

    @GetMapping("/{id}")
    public ExecutionResponse getExecution(@PathVariable Long id) {
        return executionService.getExecution(id);
    }

    @ExceptionHandler(ExecutionNotFoundException.class)
    ResponseEntity<Map<String, String>> handleExecutionNotFound(ExecutionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
                .orElse("Invalid execution request");

        return ResponseEntity.badRequest()
                .body(Map.of("message", message));
    }
}
