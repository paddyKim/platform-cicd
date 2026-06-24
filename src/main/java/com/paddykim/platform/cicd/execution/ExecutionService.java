package com.paddykim.platform.cicd.execution;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final GitOpsImageTagService gitOpsImageTagService;
    private final Map<String, BuildImageRunner> buildImageRunners;

    public ExecutionService(
            ExecutionRepository executionRepository,
            GitOpsImageTagService gitOpsImageTagService,
            List<BuildImageRunner> buildImageRunners
    ) {
        this.executionRepository = executionRepository;
        this.gitOpsImageTagService = gitOpsImageTagService;
        this.buildImageRunners = buildImageRunners.stream()
                .collect(Collectors.toMap(
                        runner -> runner.ciTool().toUpperCase(Locale.ROOT),
                        Function.identity()
                ));
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
                    null,
                    null
            );
        } catch (GitOpsExecutionException exception) {
            return failed(id, request, exception.getMessage());
        }
    }

    private ExecutionRecord buildImage(Long id, ExecutionCreateRequest request) {
        BuildImageRunner runner = buildImageRunners.get(blankToEmpty(request.ciTool()).toUpperCase(Locale.ROOT));
        if (runner == null) {
            return failed(id, request, "CI tool execution is not implemented for this runner: " + request.ciTool());
        }

        BuildExecutionResult result = runner.run(id, request);
        return record(
                id,
                request,
                result.status(),
                result.statusMessage(),
                null,
                result.cloneResult(),
                result.startedAt(),
                result.finishedAt(),
                result.exitCode(),
                result.logSummary()
        );
    }

    private static ExecutionRecord failed(Long id, ExecutionCreateRequest request, String message) {
        return record(id, request, ExecutionStatus.FAILED, message, null, null, null, null, null, null);
    }

    private static ExecutionRecord record(
            Long id,
            ExecutionCreateRequest request,
            ExecutionStatus status,
            String statusMessage,
            String changedFilePath,
            GitCloneResult cloneResult,
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
                blankToNull(request.branch()),
                blankToNull(request.workingDirectory()),
                cloneResult == null ? null : cloneResult.status().name(),
                cloneResult == null ? null : cloneResult.message(),
                cloneResult == null || cloneResult.checkoutPath() == null ? null : cloneResult.checkoutPath().toString(),
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
