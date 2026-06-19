package com.paddykim.platform.cicd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PlatformCicdApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void healthResponseIdentifiesCicdApi() {
        HealthController controller = new HealthController();

        Map<String, Object> response = controller.health();

        assertThat(response)
                .containsEntry("status", "ok")
                .containsEntry("service", "platform-cicd-api");
        assertThat(response.get("timestamp")).isInstanceOf(String.class);
    }
}
