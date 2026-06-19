package com.paddykim.platform.cicd.execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Service
public class GitOpsImageTagService {

    private final Path valuesPath;
    private final Yaml yaml;

    public GitOpsImageTagService(@Value("${platform.gitops.values-path}") String valuesPath) {
        this.valuesPath = Path.of(valuesPath);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }

    public GitOpsImageTagResult updateImageTag(ExecutionCreateRequest request) {
        validate(request);

        if (!Files.exists(valuesPath)) {
            throw new GitOpsExecutionException("GitOps values file not found: " + valuesPath);
        }

        try (InputStream inputStream = Files.newInputStream(valuesPath)) {
            Map<String, Object> values = yaml.load(inputStream);
            if (values == null) {
                values = new LinkedHashMap<>();
            }

            String componentKey = componentKey(request.componentName().trim());
            Map<String, Object> image = imageMap(values, componentKey);
            Object previousTag = image.get("tag");
            String newTag = request.requestedValue().trim();
            image.put("tag", newTag);

            try (Writer writer = Files.newBufferedWriter(valuesPath)) {
                yaml.dump(values, writer);
            }

            return new GitOpsImageTagResult(
                    valuesPath.toString(),
                    previousTag == null ? "" : previousTag.toString(),
                    newTag,
                    componentKey + ".image.tag"
            );
        } catch (IOException exception) {
            throw new GitOpsExecutionException("Unable to update GitOps values file: " + valuesPath, exception);
        }
    }

    private static void validate(ExecutionCreateRequest request) {
        if (request.requestType() != ExecutionRequestType.DEPLOY_IMAGE) {
            throw new GitOpsExecutionException("Unsupported execution request type for Day19: " + request.requestType());
        }
        if (!"dev".equalsIgnoreCase(request.environment().trim())) {
            throw new GitOpsExecutionException("Unsupported environment for Day19: " + request.environment());
        }
        componentKey(request.componentName().trim());
    }

    private static String componentKey(String componentName) {
        return switch (componentName) {
            case "platform-api" -> "api";
            case "platform-web" -> "web";
            default -> throw new GitOpsExecutionException("Unsupported component for image deployment: " + componentName);
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> imageMap(Map<String, Object> values, String componentKey) {
        Object component = values.get(componentKey);
        if (!(component instanceof Map<?, ?> componentMap)) {
            throw new GitOpsExecutionException("Missing component values: " + componentKey);
        }

        Object image = componentMap.get("image");
        if (!(image instanceof Map<?, ?> imageMap)) {
            throw new GitOpsExecutionException("Missing image values: " + componentKey + ".image");
        }

        return (Map<String, Object>) imageMap;
    }
}
