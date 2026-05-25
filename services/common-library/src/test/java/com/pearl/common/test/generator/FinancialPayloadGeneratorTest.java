package com.pearl.common.test.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.pearl.common.test.generator.FinancialPayloadGenerator.FinancialPayloadOptions;
import com.pearl.common.util.JsonUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FinancialPayloadGeneratorTest {

    @TempDir
    private Path tempDir;

    @Test
    void generatesSeededFinancialPayloadWithPaginationAndScenarioMetadata() throws Exception {
        FinancialPayloadGenerator generator = new FinancialPayloadGenerator();
        FinancialPayloadOptions options = FinancialPayloadOptions.small(91001L)
                .withSize(2, 5)
                .withRates(0.25D, 0.25D, 0.25D);

        Path first = tempDir.resolve("financial-one.json");
        Path second = tempDir.resolve("financial-two.json");
        PayloadGenerationMetrics metrics = generator.writePayload(first, options);
        generator.writePayload(second, options);

        JsonNode payload = JsonUtil.objectMapper().readTree(first.toFile());

        assertEquals(10, metrics.recordCount());
        assertEquals("FINANCIAL", metrics.payloadType());
        assertEquals(2, payload.path("totalPages").asInt());
        assertEquals(10, payload.path("totalMembers").asInt());
        assertEquals(2, payload.path("pages").size());
        assertEquals(5, payload.path("pages").get(0).path("members").size());
        assertFalse(payload.path("pages").get(0).path("subPages").isEmpty());
        assertTrue(payload.path("scenarioSummary").path("malformedRecords").isInt());
        assertEquals(Files.readString(first), Files.readString(second));
    }
}
