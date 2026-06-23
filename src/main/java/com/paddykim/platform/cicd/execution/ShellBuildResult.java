package com.paddykim.platform.cicd.execution;

import java.time.Instant;

public record ShellBuildResult(
        ExecutionStatus status,
        String statusMessage,
        Instant startedAt,
        Instant finishedAt,
        Integer exitCode,
        String logSummary
) {
}
