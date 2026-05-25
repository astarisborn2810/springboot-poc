package com.pearl.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pearl.common.dto.AuditMetadata;
import com.pearl.common.dto.BaseEvent;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonUtilTest {

    @Test
    void serializesAndDeserializesJavaTimeDtos() {
        BaseEvent event = new BaseEvent(
                "event-1",
                "batch.started",
                "corr-1",
                "batch-1",
                "vendor-1",
                "plan-1",
                "exec-1",
                "req-1",
                Instant.parse("2026-05-24T09:00:00Z"),
                AuditMetadata.system("unit-test"),
                Map.of("k", "v"));

        String json = JsonUtil.toJson(event);
        BaseEvent result = JsonUtil.fromJson(json, BaseEvent.class);

        assertTrue(json.contains("\"correlationId\":\"corr-1\""));
        assertEquals(event.eventId(), result.eventId());
        assertEquals(event.occurredAt(), result.occurredAt());
    }
}
