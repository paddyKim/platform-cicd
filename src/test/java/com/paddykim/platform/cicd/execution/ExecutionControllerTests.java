package com.paddykim.platform.cicd.execution;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionControllerTests {

    private static final Path TEST_VALUES_PATH = Path.of("build/test-values/dev-values.yaml");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExecutionRepository executionRepository;

    @BeforeEach
    void setUp() throws Exception {
        executionRepository.deleteAll();
        Files.createDirectories(TEST_VALUES_PATH.getParent());
        Files.writeString(TEST_VALUES_PATH, """
                nameOverride: platform
                imagePullSecrets:
                  - ghcr-token

                api:
                  replicaCount: 1
                  image:
                    repository: ghcr.io/paddykim/platform-api
                    tag: 1fd847c

                web:
                  replicaCount: 1
                  image:
                    repository: ghcr.io/paddykim/platform-web
                    tag: 1fd847c

                mariadb:
                  database: platform
                """);
    }

    @Test
    void deploysApiImageTagThroughGitOpsValues() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 1,
                                  "applicationName": "platform-app",
                                  "environment": "dev",
                                  "componentName": "platform-api",
                                  "requestType": "DEPLOY_IMAGE",
                                  "requestedValue": "def5678",
                                  "requestedBy": "platform-operator"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.executionId", is(1)))
                .andExpect(jsonPath("$.portalRequestId", is(1)))
                .andExpect(jsonPath("$.applicationName", is("platform-app")))
                .andExpect(jsonPath("$.environment", is("dev")))
                .andExpect(jsonPath("$.componentName", is("platform-api")))
                .andExpect(jsonPath("$.requestType", is("DEPLOY_IMAGE")))
                .andExpect(jsonPath("$.requestedValue", is("def5678")))
                .andExpect(jsonPath("$.requestedBy", is("platform-operator")))
                .andExpect(jsonPath("$.status", is("SUCCEEDED")))
                .andExpect(jsonPath("$.statusMessage", is("Updated api.image.tag from 1fd847c to def5678")))
                .andExpect(jsonPath("$.changedFilePath", is("build/test-values/dev-values.yaml")));

        assertThat(Files.readString(TEST_VALUES_PATH))
                .contains("api:")
                .contains("tag: def5678");
    }

    @Test
    void deploysWebImageTagThroughGitOpsValues() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 3,
                                  "applicationName": "platform-app",
                                  "environment": "dev",
                                  "componentName": "platform-web",
                                  "requestType": "DEPLOY_IMAGE",
                                  "requestedValue": "abc1234",
                                  "requestedBy": "platform-operator"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.componentName", is("platform-web")))
                .andExpect(jsonPath("$.status", is("SUCCEEDED")))
                .andExpect(jsonPath("$.statusMessage", is("Updated web.image.tag from 1fd847c to abc1234")));

        assertThat(Files.readString(TEST_VALUES_PATH))
                .contains("web:")
                .contains("tag: abc1234");
    }

    @Test
    void listsAndReadsExecutionRequests() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 2,
                                  "applicationName": "platform-app",
                                  "environment": "dev",
                                  "componentName": "platform-web",
                                  "requestType": "BUILD_IMAGE",
                                  "requestedValue": "main",
                                  "requestedBy": "platform-operator"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cicd/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].portalRequestId", is(2)));

        mockMvc.perform(get("/api/cicd/executions/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", is(1)))
                .andExpect(jsonPath("$.requestType", is("BUILD_IMAGE")))
                .andExpect(jsonPath("$.status", is("FAILED")));
    }

    @Test
    void runsShellBuildProfileExecution() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 7,
                                  "applicationName": "platform-app",
                                  "environment": "dev",
                                  "componentName": "platform-api",
                                  "requestType": "BUILD_IMAGE",
                                  "requestedValue": "day21-test",
                                  "requestedBy": "platform-operator",
                                  "sourceRepositoryId": 10,
                                  "buildProfileId": 20,
                                  "ciTool": "SHELL",
                                  "repositoryUrl": "https://github.com/paddyKim/platform-app.git",
                                  "workingDirectory": ".",
                                  "script": "echo building $REPOSITORY_URL with $IMAGE_TAG"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portalRequestId", is(7)))
                .andExpect(jsonPath("$.requestType", is("BUILD_IMAGE")))
                .andExpect(jsonPath("$.sourceRepositoryId", is(10)))
                .andExpect(jsonPath("$.buildProfileId", is(20)))
                .andExpect(jsonPath("$.ciTool", is("SHELL")))
                .andExpect(jsonPath("$.repositoryUrl", is("https://github.com/paddyKim/platform-app.git")))
                .andExpect(jsonPath("$.workingDirectory", is(".")))
                .andExpect(jsonPath("$.status", is("SUCCEEDED")))
                .andExpect(jsonPath("$.statusMessage", is("Shell script completed with exit code 0")))
                .andExpect(jsonPath("$.exitCode", is(0)))
                .andExpect(jsonPath("$.logSummary", containsString("building https://github.com/paddyKim/platform-app.git")))
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.finishedAt").exists());
    }

    @Test
    void failsShellBuildProfileExecutionWhenScriptExitsNonZero() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 8,
                                  "applicationName": "platform-app",
                                  "environment": "dev",
                                  "componentName": "platform-api",
                                  "requestType": "BUILD_IMAGE",
                                  "requestedValue": "day22-test",
                                  "requestedBy": "platform-operator",
                                  "sourceRepositoryId": 10,
                                  "buildProfileId": 20,
                                  "ciTool": "SHELL",
                                  "repositoryUrl": "https://github.com/paddyKim/platform-app.git",
                                  "workingDirectory": ".",
                                  "script": "echo failing build && exit 7"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.statusMessage", is("Shell script completed with exit code 7")))
                .andExpect(jsonPath("$.exitCode", is(7)))
                .andExpect(jsonPath("$.logSummary", containsString("failing build")));
    }

    @Test
    void failsBuildProfileExecutionForUnsupportedCiTool() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 9,
                                  "applicationName": "platform-app",
                                  "environment": "dev",
                                  "componentName": "platform-api",
                                  "requestType": "BUILD_IMAGE",
                                  "requestedValue": "day22-test",
                                  "requestedBy": "platform-operator",
                                  "sourceRepositoryId": 10,
                                  "buildProfileId": 20,
                                  "ciTool": "GITHUB_ACTIONS",
                                  "repositoryUrl": "https://github.com/paddyKim/platform-app.git",
                                  "workingDirectory": ".",
                                  "script": "name: build"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.statusMessage", containsString("not implemented")));
    }

    @Test
    void failsUnsupportedComponent() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 4,
                                  "applicationName": "platform-app",
                                  "environment": "dev",
                                  "componentName": "platform-mariadb",
                                  "requestType": "DEPLOY_IMAGE",
                                  "requestedValue": "11.4",
                                  "requestedBy": "platform-operator"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.statusMessage", containsString("Unsupported component")));
    }

    @Test
    void failsUnsupportedEnvironment() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 5,
                                  "applicationName": "platform-app",
                                  "environment": "stg",
                                  "componentName": "platform-api",
                                  "requestType": "DEPLOY_IMAGE",
                                  "requestedValue": "abc1234",
                                  "requestedBy": "platform-operator"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.statusMessage", containsString("Unsupported environment")));
    }

    @Test
    void rejectsInvalidExecutionRequest() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 0,
                                  "applicationName": "",
                                  "environment": "dev",
                                  "componentName": "platform-api",
                                  "requestType": "DEPLOY_IMAGE",
                                  "requestedValue": "1fd847c",
                                  "requestedBy": "platform-operator"
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must")));
    }

    @Test
    void returnsNotFoundForUnknownExecution() throws Exception {
        mockMvc.perform(get("/api/cicd/executions/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("CI/CD execution not found: 9999")));
    }
}
