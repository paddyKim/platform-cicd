package com.paddykim.platform.cicd.execution;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExecutionService {

    private final ExecutionRepository executionRepository;

    public ExecutionService(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    public ExecutionResponse createExecution(ExecutionCreateRequest request) {
        return ExecutionResponse.from(executionRepository.save(request));
    }

    public ExecutionResponse getExecution(Long id) {
        ExecutionRecord record = executionRepository.findById(id)
                .orElseThrow(() -> new ExecutionNotFoundException(id));

        return ExecutionResponse.from(record);
    }

    public List<ExecutionResponse> listExecutions() {
        return executionRepository.findAll().stream()
                .map(ExecutionResponse::from)
                .toList();
    }
}
