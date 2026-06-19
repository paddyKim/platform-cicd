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
        ExecutionStatus status,
        String statusMessage,
        String changedFilePath,
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
                record.getStatus(),
                record.getStatusMessage(),
                record.getChangedFilePath(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
