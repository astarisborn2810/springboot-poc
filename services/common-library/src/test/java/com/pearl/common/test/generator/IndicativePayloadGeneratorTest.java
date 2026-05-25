package com.pearl.common.test.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.pearl.common.test.generator.IndicativePayloadGenerator.IndicativePayloadOptions;
import com.pearl.common.util.JsonUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndicativePayloadGeneratorTest {

    @TempDir
    private Path tempDir;

    @Test
    void generatesSeededIndicativePayloadWithNestedParticipantStructures() throws Exception {
        IndicativePayloadGenerator generator = new IndicativePayloadGenerator();
        IndicativePayloadOptions options = IndicativePayloadOptions.small(92001L)
                .withEntityCount(12)
                .withRates(0.25D, 0.25D, 0.25D, 0.25D);

        Path first = tempDir.resolve("indicative-one.json");
        Path second = tempDir.resolve("indicative-two.json");
        PayloadGenerationMetrics metrics = generator.writePayload(first, options);
        generator.writePayload(second, options);

        JsonNode payload = JsonUtil.objectMapper().readTree(first.toFile());
        JsonNode firstEntity = payload.path("entities").get(0);

        assertEquals(12, metrics.recordCount());
        assertEquals("INDICATIVE", metrics.payloadType());
        assertEquals(12, payload.path("entityCount").asInt());
        assertEquals(12, payload.path("entities").size());
        assertTrue(firstEntity.has("identity"));
        assertTrue(firstEntity.has("addresses"));
        assertTrue(firstEntity.path("dependents").has("beneficiaries"));
        assertTrue(payload.path("scenarioSummary").path("duplicateParticipantIds").isInt());
        assertEquals(Files.readString(first), Files.readString(second));
    }
}
