package com.paddykim.platform.cicd.execution;

import org.springframework.stereotype.Service;

@Service
public class ShellBuildImageRunner implements BuildImageRunner {

    private final GitCloneService gitCloneService;
    private final ShellBuildRunner shellBuildRunner;

    public ShellBuildImageRunner(
            GitCloneService gitCloneService,
            ShellBuildRunner shellBuildRunner
    ) {
        this.gitCloneService = gitCloneService;
        this.shellBuildRunner = shellBuildRunner;
    }

    @Override
    public String ciTool() {
        return "SHELL";
    }

    @Override
    public BuildExecutionResult run(Long executionId, ExecutionCreateRequest request) {
        GitCloneResult cloneResult = gitCloneService.cloneRepository(executionId, request);
        if (cloneResult.status() != ExecutionStatus.SUCCEEDED) {
            return BuildExecutionResult.cloneFailed(cloneResult);
        }

        ShellBuildResult shellBuildResult = shellBuildRunner.run(request, cloneResult.checkoutPath());
        return BuildExecutionResult.fromShell(cloneResult, shellBuildResult);
    }
}
