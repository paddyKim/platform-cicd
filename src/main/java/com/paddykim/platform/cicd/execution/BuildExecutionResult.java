package com.paddykim.platform.cicd.execution;

import java.time.Instant;

public record BuildExecutionResult(
        ExecutionStatus status,
        String statusMessage,
        GitCloneResult cloneResult,
        Instant startedAt,
        Instant finishedAt,
        Integer exitCode,
        String logSummary
) {

    static BuildExecutionResult fromShell(GitCloneResult cloneResult, ShellBuildResult shellBuildResult) {
        return new BuildExecutionResult(
                shellBuildResult.status(),
                shellBuildResult.statusMessage(),
                cloneResult,
                shellBuildResult.startedAt(),
                shellBuildResult.finishedAt(),
                shellBuildResult.exitCode(),
                shellBuildResult.logSummary()
        );
    }

    static BuildExecutionResult cloneFailed(GitCloneResult cloneResult) {
        return new BuildExecutionResult(
                ExecutionStatus.FAILED,
                cloneResult.message(),
                cloneResult,
                null,
                null,
                null,
                null
        );
    }
}
