package com.paddykim.platform.cicd.execution;

public class ExecutionNotFoundException extends RuntimeException {

    public ExecutionNotFoundException(Long id) {
        super("CI/CD execution not found: " + id);
    }
}
