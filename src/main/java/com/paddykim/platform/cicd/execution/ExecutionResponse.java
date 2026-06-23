package com.paddykim.platform.cicd.execution;

import java.time.Instant;

public record ExecutionResponse(
        Long executionId,
        Long portalRequestId,
        String applicationName,
        String environment,
        String componentName,
        ExecutionRequestType requestType,
        String requestedValue,
        String requestedBy,
        Long sourceRepositoryId,
        Long buildProfileId,
        String ciTool,
        String repositoryUrl,
        String workingDirectory,
        ExecutionStatus status,
        String statusMessage,
        String changedFilePath,
        Instant startedAt,
        Instant finishedAt,
        Integer exitCode,
        String logSummary,
        Instant createdAt,
        Instant updatedAt
) {

    static ExecutionResponse from(ExecutionRecord record) {
        return new ExecutionResponse(
                record.getId(),
                record.getPortalRequestId(),
                record.getApplicationName(),
                record.getEnvironment(),
                record.getComponentName(),
                record.getRequestType(),
                record.getRequestedValue(),
                record.getRequestedBy(),
                record.getSourceRepositoryId(),
                record.getBuildProfileId(),
                record.getCiTool(),
                record.getRepositoryUrl(),
                record.getWorkingDirectory(),
                record.getStatus(),
                record.getStatusMessage(),
                record.getChangedFilePath(),
                record.getStartedAt(),
                record.getFinishedAt(),
                record.getExitCode(),
                record.getLogSummary(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
