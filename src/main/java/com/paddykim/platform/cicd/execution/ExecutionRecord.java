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
    private final ExecutionStatus status;
    private final String statusMessage;
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
            ExecutionStatus status,
            String statusMessage
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
        this.status = status;
        this.statusMessage = statusMessage;
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

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
