package com.paddykim.platform.cicd.execution;

public record GitOpsImageTagResult(
        String changedFilePath,
        String previousTag,
        String newTag,
        String valuesKey
) {
}
