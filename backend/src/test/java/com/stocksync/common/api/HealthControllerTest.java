package com.stocksync.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;

class HealthControllerTest {

    @Test
    void returnsActuatorHealthStatus() {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        HealthController controller = new HealthController(healthEndpoint);

        assertThat(controller.health()).containsEntry("status", "UP");
    }
}
