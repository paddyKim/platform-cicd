package com.paddykim.platform.cicd.execution;

import java.time.Instant;

public class ExecutionRecord {

    private final Long id;
    private final Long portalRequestId;
    private final String applicationName;
    private final String environment;
    private final String componentName;
    private final ExecutionRequestType requestType;
    private final String requestedValue;
    private final String requestedBy;
    private final Long sourceRepositoryId;
    private final Long buildProfileId;
    private final String ciTool;
    private final String repositoryUrl;
    private final String workingDirectory;
    private final ExecutionStatus status;
    private final String statusMessage;
    private final String changedFilePath;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Integer exitCode;
    private final String logSummary;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ExecutionRecord(
            Long id,
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
            String logSummary
    ) {
        Instant now = Instant.now();
        this.id = id;
        this.portalRequestId = portalRequestId;
        this.applicationName = applicationName;
        this.environment = environment;
        this.componentName = componentName;
        this.requestType = requestType;
        this.requestedValue = requestedValue;
        this.requestedBy = requestedBy;
        this.sourceRepositoryId = sourceRepositoryId;
        this.buildProfileId = buildProfileId;
        this.ciTool = ciTool;
        this.repositoryUrl = repositoryUrl;
        this.workingDirectory = workingDirectory;
        this.status = status;
        this.statusMessage = statusMessage;
        this.changedFilePath = changedFilePath;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.exitCode = exitCode;
        this.logSummary = logSummary;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getPortalRequestId() {
        return portalRequestId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getComponentName() {
        return componentName;
    }

    public ExecutionRequestType getRequestType() {
        return requestType;
    }

    public String getRequestedValue() {
        return requestedValue;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Long getSourceRepositoryId() {
        return sourceRepositoryId;
    }

    public Long getBuildProfileId() {
        return buildProfileId;
    }

    public String getCiTool() {
        return ciTool;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getChangedFilePath() {
        return changedFilePath;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getLogSummary() {
        return logSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
