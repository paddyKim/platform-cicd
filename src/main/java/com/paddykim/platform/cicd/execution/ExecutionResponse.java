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
        String branch,
        String workingDirectory,
        String cloneStatus,
        String cloneMessage,
        String checkoutPath,
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
                record.getBranch(),
                record.getWorkingDirectory(),
                record.getCloneStatus(),
                record.getCloneMessage(),
                record.getCheckoutPath(),
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
