package com.paddykim.platform.cicd.execution;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitCloneService {

    private final Path workspaceRoot;
    private final Duration timeout;
    private final int logLimitBytes;

    public GitCloneService(
            @Value("${platform.runner.workspace-root:build/cicd-workspaces}") String workspaceRoot,
            @Value("${platform.runner.timeout-seconds:30}") long timeoutSeconds,
            @Value("${platform.runner.log-limit-bytes:12000}") int logLimitBytes
    ) {
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.logLimitBytes = logLimitBytes;
    }

    public GitCloneResult cloneRepository(Long executionId, ExecutionCreateRequest request) {
        String repositoryUrl = requireText(request.repositoryUrl(), "repositoryUrl");
        String branch = normalizeBranch(request.branch());
        String credential = blankToNull(request.credential());
        String cloneUrl = credential == null
                ? repositoryUrl
                : authenticatedCloneUrl(repositoryUrl, request.accountName(), credential);
        Path executionRoot = workspaceRoot.resolve("execution-%d".formatted(executionId)).normalize();
        Path checkoutPath = executionRoot.resolve("repository").normalize();
        ExecutorService outputExecutor = Executors.newSingleThreadExecutor();
        Future<String> outputFuture = null;

        try {
            resetExecutionWorkspace(executionRoot);
            Files.createDirectories(executionRoot);

            Process process = new ProcessBuilder(
                    "git",
                    "clone",
                    "--branch",
                    branch,
                    "--single-branch",
                    cloneUrl,
                    checkoutPath.toString()
            )
                    .redirectErrorStream(true)
                    .start();

            outputFuture = outputExecutor.submit(new OutputCollector(process.getInputStream(), logLimitBytes));
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new GitCloneResult(
                        ExecutionStatus.FAILED,
                        "Git clone timed out after %d seconds".formatted(timeout.toSeconds()),
                        branch,
                        checkoutPath
                );
            }

            String summary = mask(waitForOutput(outputFuture), credential, cloneUrl);
            if (process.exitValue() != 0) {
                return new GitCloneResult(
                        ExecutionStatus.FAILED,
                        "Git clone failed with exit code %d: %s".formatted(process.exitValue(), summary.trim()),
                        branch,
                        checkoutPath
                );
            }

            return new GitCloneResult(
                    ExecutionStatus.SUCCEEDED,
                    "Git clone completed for branch %s".formatted(branch),
                    branch,
                    checkoutPath
            );
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new GitCloneResult(
                    ExecutionStatus.FAILED,
                    "Git clone failed: " + mask(exception.getMessage(), credential, cloneUrl),
                    branch,
                    checkoutPath
            );
        } finally {
            outputExecutor.shutdownNow();
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new GitOpsExecutionException("Build " + field + " is required");
        }

        return value.trim();
    }

    private static String normalizeBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return "main";
        }

        return branch.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static String authenticatedCloneUrl(String repositoryUrl, String accountName, String credential) {
        String account = requireText(accountName, "accountName");
        try {
            URI uri = new URI(repositoryUrl);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new GitOpsExecutionException("Authenticated clone supports only HTTP(S) repository URLs");
            }

            return new URI(
                    uri.getScheme(),
                    account + ":" + credential,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException exception) {
            throw new GitOpsExecutionException("Repository URL is invalid", exception);
        }
    }

    private static void resetExecutionWorkspace(Path executionRoot) throws IOException {
        if (!Files.exists(executionRoot)) {
            return;
        }

        try (var paths = Files.walk(executionRoot)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static String waitForOutput(Future<String> outputFuture) {
        try {
            return outputFuture == null ? "" : outputFuture.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException exception) {
            return "";
        }
    }

    private static class OutputCollector implements Callable<String> {

        private final InputStream inputStream;
        private final int logLimitBytes;

        OutputCollector(InputStream inputStream, int logLimitBytes) {
            this.inputStream = inputStream;
            this.logLimitBytes = logLimitBytes;
        }

        @Override
        public String call() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(logLimitBytes, 0));
            copyLimited(inputStream, output, logLimitBytes);
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private static void copyLimited(InputStream inputStream, ByteArrayOutputStream output, int logLimitBytes) throws IOException {
        byte[] buffer = new byte[1024];
        boolean truncated = false;
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            int remaining = logLimitBytes - output.size();
            if (remaining > 0) {
                output.write(buffer, 0, Math.min(read, remaining));
            }
            if (read > remaining) {
                truncated = true;
            }
        }

        if (truncated) {
            output.write("\n[log truncated]".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String mask(String value, String credential, String cloneUrl) {
        if (value == null) {
            return "";
        }

        String masked = value;
        if (credential != null && !credential.isBlank()) {
            masked = masked.replace(credential, "***");
        }
        if (cloneUrl != null && !cloneUrl.isBlank()) {
            masked = masked.replace(cloneUrl, "***");
        }

        return masked;
    }
}
