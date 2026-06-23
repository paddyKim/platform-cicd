package com.paddykim.platform.cicd.execution;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final GitOpsImageTagService gitOpsImageTagService;
    private final ShellBuildRunner shellBuildRunner;

    public ExecutionService(
            ExecutionRepository executionRepository,
            GitOpsImageTagService gitOpsImageTagService,
            ShellBuildRunner shellBuildRunner
    ) {
        this.executionRepository = executionRepository;
        this.gitOpsImageTagService = gitOpsImageTagService;
        this.shellBuildRunner = shellBuildRunner;
    }

    public ExecutionResponse createExecution(ExecutionCreateRequest request) {
        Long id = executionRepository.nextId();
        ExecutionRecord record = switch (request.requestType()) {
            case DEPLOY_IMAGE -> deployImage(id, request);
            case BUILD_IMAGE -> buildImage(id, request);
            case CHANGE_REPLICAS -> failed(id, request, "Unsupported execution request type: " + request.requestType());
        };

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

    private ExecutionRecord deployImage(Long id, ExecutionCreateRequest request) {
        try {
            GitOpsImageTagResult result = gitOpsImageTagService.updateImageTag(request);
            return record(
                    id,
                    request,
                    ExecutionStatus.SUCCEEDED,
                    "Updated %s from %s to %s".formatted(
                            result.valuesKey(),
                            result.previousTag(),
                            result.newTag()
                    ),
                    result.changedFilePath(),
                    null,
                    null,
                    null,
                    null
            );
        } catch (GitOpsExecutionException exception) {
            return failed(id, request, exception.getMessage());
        }
    }

    private ExecutionRecord buildImage(Long id, ExecutionCreateRequest request) {
        if (!"SHELL".equalsIgnoreCase(blankToEmpty(request.ciTool()))) {
            return failed(id, request, "CI tool execution is not implemented for Day22: " + request.ciTool());
        }

        ShellBuildResult result = shellBuildRunner.run(id, request);
        return record(
                id,
                request,
                result.status(),
                result.statusMessage(),
                null,
                result.startedAt(),
                result.finishedAt(),
                result.exitCode(),
                result.logSummary()
        );
    }

    private static ExecutionRecord failed(Long id, ExecutionCreateRequest request, String message) {
        return record(id, request, ExecutionStatus.FAILED, message, null, null, null, null, null);
    }

    private static ExecutionRecord record(
            Long id,
            ExecutionCreateRequest request,
            ExecutionStatus status,
            String statusMessage,
            String changedFilePath,
            java.time.Instant startedAt,
            java.time.Instant finishedAt,
            Integer exitCode,
            String logSummary
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
                request.sourceRepositoryId(),
                request.buildProfileId(),
                blankToNull(request.ciTool()),
                blankToNull(request.repositoryUrl()),
                blankToNull(request.workingDirectory()),
                status,
                statusMessage,
                changedFilePath,
                startedAt,
                finishedAt,
                exitCode,
                logSummary
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
