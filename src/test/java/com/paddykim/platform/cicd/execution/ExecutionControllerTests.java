package com.paddykim.platform.cicd.execution;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExecutionRepository executionRepository;

    @BeforeEach
    void setUp() {
        executionRepository.deleteAll();
    }

    @Test
    void createsExecutionRequest() throws Exception {
        mockMvc.perform(post("/api/cicd/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portalRequestId": 1,
                                  "applicationName": "platform-app",
                                  "environment": "dev",
                                  "componentName": "platform-api",
                                  "requestType": "DEPLOY_IMAGE",
                                  "requestedValue": "1fd847c",
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
                .andExpect(jsonPath("$.requestedValue", is("1fd847c")))
                .andExpect(jsonPath("$.requestedBy", is("platform-operator")))
                .andExpect(jsonPath("$.status", is("REQUESTED")))
                .andExpect(jsonPath("$.statusMessage", is("Execution request received by platform-cicd")));
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
                .andExpect(jsonPath("$.requestType", is("BUILD_IMAGE")));
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
