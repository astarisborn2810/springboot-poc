package com.pearl.common.test.generator;

import java.nio.file.Path;

public record PayloadGenerationMetrics(
        Path file,
        String payloadType,
        long bytes,
        int recordCount,
        int pageCount,
        int recommendedRecordsPerChunk,
        int recommendedS3Parts,
        String recommendation) {
}
