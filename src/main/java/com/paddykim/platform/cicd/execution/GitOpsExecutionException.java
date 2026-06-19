package com.paddykim.platform.cicd.execution;

public class GitOpsExecutionException extends RuntimeException {

    public GitOpsExecutionException(String message) {
        super(message);
    }

    public GitOpsExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
