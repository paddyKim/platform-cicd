package com.paddykim.platform.cicd.execution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExecutionCreateRequest(
        @NotNull @Positive Long portalRequestId,
        @NotBlank String applicationName,
        @NotBlank String environment,
        @NotBlank String componentName,
        @NotNull ExecutionRequestType requestType,
        @NotBlank String requestedValue,
        @NotBlank String requestedBy,
        Long sourceRepositoryId,
        Long buildProfileId,
        String ciTool,
        String repositoryUrl,
        String workingDirectory,
        String script
) {
}
