package com.paddykim.platform.cicd.execution;

import java.nio.file.Path;

public record GitCloneResult(
        ExecutionStatus status,
        String message,
        String branch,
        Path checkoutPath
) {
}
