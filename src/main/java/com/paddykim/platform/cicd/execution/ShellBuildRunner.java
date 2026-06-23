package com.paddykim.platform.cicd.execution;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
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
public class ShellBuildRunner {

    private final Path workspaceRoot;
    private final Duration timeout;
    private final int logLimitBytes;

    public ShellBuildRunner(
            @Value("${platform.runner.workspace-root:build/cicd-workspaces}") String workspaceRoot,
            @Value("${platform.runner.timeout-seconds:30}") long timeoutSeconds,
            @Value("${platform.runner.log-limit-bytes:12000}") int logLimitBytes
    ) {
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.logLimitBytes = logLimitBytes;
    }

    public ShellBuildResult run(Long executionId, ExecutionCreateRequest request) {
        Instant startedAt = Instant.now();
        Process process = null;
        ExecutorService outputExecutor = Executors.newSingleThreadExecutor();
        Future<LogCapture> outputFuture = null;

        try {
            String script = requireText(request.script(), "script");
            String workingDirectory = request.workingDirectory() == null || request.workingDirectory().isBlank()
                    ? "."
                    : request.workingDirectory().trim();
            Path executionRoot = workspaceRoot.resolve("execution-%d".formatted(executionId)).normalize();
            resetExecutionWorkspace(executionRoot);
            Path runDirectory = resolveWorkingDirectory(executionRoot, workingDirectory);
            Files.createDirectories(runDirectory);

            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-euo", "pipefail", "-c", script)
                    .directory(runDirectory.toFile())
                    .redirectErrorStream(true);
            configureEnvironment(processBuilder.environment(), request);

            process = processBuilder.start();
            outputFuture = outputExecutor.submit(new OutputCollector(process.getInputStream(), logLimitBytes));

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            Instant finishedAt = Instant.now();
            if (!finished) {
                process.destroyForcibly();
                LogCapture logCapture = waitForOutput(outputFuture);
                return new ShellBuildResult(
                        ExecutionStatus.FAILED,
                        "Shell script timed out after %d seconds".formatted(timeout.toSeconds()),
                        startedAt,
                        finishedAt,
                        null,
                        logCapture.summary()
                );
            }

            int exitCode = process.exitValue();
            LogCapture logCapture = waitForOutput(outputFuture);
            return new ShellBuildResult(
                    exitCode == 0 ? ExecutionStatus.SUCCEEDED : ExecutionStatus.FAILED,
                    "Shell script completed with exit code %d".formatted(exitCode),
                    startedAt,
                    finishedAt,
                    exitCode,
                    logCapture.summary()
            );
        } catch (RuntimeException | IOException | InterruptedException exception) {
            if (process != null) {
                process.destroyForcibly();
            }
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return new ShellBuildResult(
                    ExecutionStatus.FAILED,
                    "Shell script execution failed: " + exception.getMessage(),
                    startedAt,
                    Instant.now(),
                    null,
                    ""
            );
        } finally {
            outputExecutor.shutdownNow();
        }
    }

    private Path resolveWorkingDirectory(Path executionRoot, String workingDirectory) {
        try {
            Path relative = Path.of(workingDirectory).normalize();
            if (relative.isAbsolute() || relative.startsWith("..") || workingDirectory.contains("..")) {
                throw new GitOpsExecutionException("Build workingDirectory must stay inside execution workspace");
            }

            Path runDirectory = executionRoot.resolve(relative).normalize();
            if (!runDirectory.startsWith(executionRoot)) {
                throw new GitOpsExecutionException("Build workingDirectory must stay inside execution workspace");
            }

            return runDirectory;
        } catch (InvalidPathException exception) {
            throw new GitOpsExecutionException("Build workingDirectory is invalid", exception);
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

    private static void configureEnvironment(Map<String, String> environment, ExecutionCreateRequest request) {
        environment.put("IMAGE_TAG", request.requestedValue().trim());
        environment.put("APPLICATION_NAME", request.applicationName().trim());
        environment.put("ENVIRONMENT", request.environment().trim());
        environment.put("COMPONENT_NAME", request.componentName().trim());
        environment.put("CI_TOOL", request.ciTool() == null ? "" : request.ciTool().trim());
        environment.put("REPOSITORY_URL", request.repositoryUrl() == null ? "" : request.repositoryUrl().trim());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new GitOpsExecutionException("Build " + field + " is required");
        }

        return value;
    }

    private static LogCapture waitForOutput(Future<LogCapture> outputFuture) {
        try {
            return outputFuture.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new LogCapture("", false);
        } catch (ExecutionException | TimeoutException exception) {
            return new LogCapture("", false);
        }
    }

    private record LogCapture(String summary, boolean truncated) {
    }

    private static class OutputCollector implements Callable<LogCapture> {

        private final InputStream inputStream;
        private final int limitBytes;

        OutputCollector(InputStream inputStream, int limitBytes) {
            this.inputStream = inputStream;
            this.limitBytes = limitBytes;
        }

        @Override
        public LogCapture call() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(limitBytes, 0));
            byte[] buffer = new byte[1024];
            boolean truncated = false;
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                int remaining = limitBytes - output.size();
                if (remaining > 0) {
                    output.write(buffer, 0, Math.min(read, remaining));
                }
                if (read > remaining) {
                    truncated = true;
                }
            }

            String summary = output.toString(StandardCharsets.UTF_8);
            if (truncated) {
                summary = summary + "\n[log truncated]";
            }

            return new LogCapture(summary, truncated);
        }
    }
}
