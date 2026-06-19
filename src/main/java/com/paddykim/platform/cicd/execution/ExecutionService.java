package com.paddykim.platform.cicd.execution;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final GitOpsImageTagService gitOpsImageTagService;

    public ExecutionService(
            ExecutionRepository executionRepository,
            GitOpsImageTagService gitOpsImageTagService
    ) {
        this.executionRepository = executionRepository;
        this.gitOpsImageTagService = gitOpsImageTagService;
    }

    public ExecutionResponse createExecution(ExecutionCreateRequest request) {
        Long id = executionRepository.nextId();
        ExecutionRecord record;

        try {
            GitOpsImageTagResult result = gitOpsImageTagService.updateImageTag(request);
            record = record(
                    id,
                    request,
                    ExecutionStatus.SUCCEEDED,
                    "Updated %s from %s to %s".formatted(
                            result.valuesKey(),
                            result.previousTag(),
                            result.newTag()
                    ),
                    result.changedFilePath()
            );
        } catch (GitOpsExecutionException exception) {
            record = record(
                    id,
                    request,
                    ExecutionStatus.FAILED,
                    exception.getMessage(),
                    null
            );
        }

        return ExecutionResponse.from(executionRepository.save(record));
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

    private static ExecutionRecord record(
            Long id,
            ExecutionCreateRequest request,
            ExecutionStatus status,
            String statusMessage,
            String changedFilePath
    ) {
        return new ExecutionRecord(
                id,
                request.portalRequestId(),
                request.applicationName().trim(),
                request.environment().trim(),
                request.componentName().trim(),
                request.requestType(),
                request.requestedValue().trim(),
                request.requestedBy().trim(),
                status,
                statusMessage,
                changedFilePath
        );
    }
}
