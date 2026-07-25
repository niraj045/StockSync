package com.stocksync.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiErrorResponseTest {

    @Test
    void normalizesMissingFieldErrorsToAnEmptyList() {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.parse("2026-07-25T00:00:00Z"),
                500,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                null);

        assertThat(response.fieldErrors()).isEmpty();
    }
}
