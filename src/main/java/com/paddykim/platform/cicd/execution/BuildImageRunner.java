package com.paddykim.platform.cicd.execution;

public interface BuildImageRunner {

    String ciTool();

    BuildExecutionResult run(Long executionId, ExecutionCreateRequest request);
}
