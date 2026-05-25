package com.pearl.common.test.generator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.pearl.common.test.generator.FinancialPayloadGenerator.FinancialPayloadOptions;
import com.pearl.common.test.generator.IndicativePayloadGenerator.IndicativePayloadOptions;
import com.pearl.common.util.JsonUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class PayloadGeneratorCli {

    private PayloadGeneratorCli() {
    }

    public static void main(String[] args) throws IOException {
        Path workspaceRoot = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        Path payloadRoot = workspaceRoot.resolve("payloads").normalize();
        Path financialRoot = payloadRoot.resolve("financial");
        Path indicativeRoot = payloadRoot.resolve("indicative");

        FinancialPayloadGenerator financialGenerator = new FinancialPayloadGenerator();
        IndicativePayloadGenerator indicativeGenerator = new IndicativePayloadGenerator();
        List<PayloadGenerationMetrics> metrics = new ArrayList<>();

        metrics.add(financialGenerator.writePayload(
                financialRoot.resolve("financial-payload-large.json"),
                FinancialPayloadOptions.large()));
        metrics.addAll(financialGenerator.writeScenarioPayloads(financialRoot.resolve("scenarios")));
        metrics.add(indicativeGenerator.writePayload(
                indicativeRoot.resolve("indicative-payload-large.json"),
                IndicativePayloadOptions.large()));
        metrics.addAll(indicativeGenerator.writeScenarioPayloads(indicativeRoot.resolve("scenarios")));

        writeMetrics(payloadRoot.resolve("payload-metrics.json"), metrics);
    }

    private static void writeMetrics(Path outputFile, List<PayloadGenerationMetrics> metrics) throws IOException {
        Files.createDirectories(outputFile.toAbsolutePath().getParent());
        try (OutputStream output = Files.newOutputStream(outputFile);
             JsonGenerator json = JsonUtil.objectMapper().getFactory().createGenerator(output)) {
            json.useDefaultPrettyPrinter();
            json.writeStartObject();
            json.writeStringField("generatedAt", Instant.now().toString());
            json.writeStringField("generator", PayloadGeneratorCli.class.getName());
            json.writeStringField("fakerLibrary", "net.datafaker:datafaker");
            json.writeObjectFieldStart("loadTestingGuidance");
            json.writeNumberField("financialMembersPerPage", 250);
            json.writeNumberField("indicativeEntitiesPerChunk", 500);
            json.writeStringField("s3UploadMode", "store full payload in S3 and pass S3 object pointers through SQS and Step Functions");
            json.writeStringField("ecsWorkerRecommendation", "route each financial page or indicative entity chunk to an idempotent ECS worker task");
            json.writeStringField("lambdaRecommendation", "use Lambda for orchestration and validation, not for full multi-MB payload transformation");
            json.writeEndObject();
            json.writeArrayFieldStart("files");
            for (PayloadGenerationMetrics metric : metrics) {
                json.writeStartObject();
                json.writeStringField("file", metric.file().toString().replace('\\', '/'));
                json.writeStringField("payloadType", metric.payloadType());
                json.writeNumberField("bytes", metric.bytes());
                json.writeNumberField("sizeMb", Math.round((metric.bytes() / 1024.0D / 1024.0D) * 100.0D) / 100.0D);
                json.writeNumberField("recordCount", metric.recordCount());
                json.writeNumberField("pageCount", metric.pageCount());
                json.writeNumberField("recommendedRecordsPerChunk", metric.recommendedRecordsPerChunk());
                json.writeNumberField("recommendedS3Parts", metric.recommendedS3Parts());
                json.writeStringField("recommendation", metric.recommendation());
                json.writeEndObject();
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }
}
