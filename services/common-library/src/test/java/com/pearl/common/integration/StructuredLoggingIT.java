package com.pearl.common.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StructuredLoggingIT {

    @Test
    void logbackConfigurationUsesJsonEncoderAndMdc() throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream("logback.xml")) {
            assertNotNull(stream);
            String config = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(config.contains("net.logstash.logback.encoder.LogstashEncoder"));
            assertTrue(config.contains("<includeMdc>true</includeMdc>"));
            assertTrue(config.contains("pearl-payroll-platform"));
        }
    }
}
