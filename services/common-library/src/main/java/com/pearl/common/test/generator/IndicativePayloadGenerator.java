package com.pearl.common.test.generator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.pearl.common.util.JsonUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.datafaker.Faker;

public final class IndicativePayloadGenerator {

    private static final Instant PROCESSING_TIMESTAMP = Instant.parse("2026-05-22T23:52:15Z");
    private static final String[] EMPLOYMENT_STATUSES = {"ACTIVE", "ACTIVE", "ACTIVE", "ACTIVE", "LEAVE", "TERMINATED", "RETIRED"};
    private static final String[] EMPLOYMENT_TYPES = {"FULL_TIME", "FULL_TIME", "PART_TIME", "SEASONAL", "CONTRACT"};
    private static final String[] COMPENSATION_TYPES = {"SALARIED", "HOURLY", "HOURLY", "COMMISSION", "HYBRID"};
    private static final String[] MARITAL_STATUSES = {"SINGLE", "MARRIED", "MARRIED", "DIVORCED", "DOMESTIC_PARTNER", "WIDOWED"};
    private static final String[] LANGUAGES = {"en-US", "en-US", "en-US", "es-US", "fr-CA", "zh-CN", "hi-IN", "vi-VN"};
    private static final String[] CITIZENSHIP = {"US_CITIZEN", "US_CITIZEN", "US_CITIZEN", "PERMANENT_RESIDENT", "WORK_AUTHORIZED", "UNKNOWN"};
    private static final String[] STATES = {
            "AL", "AZ", "CA", "CO", "FL", "GA", "IL", "MA", "MI", "NC", "NJ", "NY", "OH", "PA", "TN", "TX", "VA", "WA"
    };
    private static final String[] MEDICAL_PLANS = {"PPO-1500", "HDHP-HSA", "EPO-500", "HMO-CLASSIC", "WAIVED"};
    private static final String[] DENTAL_PLANS = {"DENTAL-BASIC", "DENTAL-PREMIER", "WAIVED"};
    private static final String[] VISION_PLANS = {"VISION-STANDARD", "VISION-PREMIER", "WAIVED"};
    private static final String[] DISABILITY_PLANS = {"STD-EMPLOYER-PAID", "LTD-BUYUP", "WAIVED"};
    private static final String[] LIFE_PLANS = {"BASIC-LIFE-1X", "SUPPLEMENTAL-LIFE", "EXEC-LIFE", "WAIVED"};
    private static final String[] BENEFICIARY_RELATIONSHIPS = {"SPOUSE", "CHILD", "PARENT", "SIBLING", "TRUST", "OTHER"};

    public PayloadGenerationMetrics writePayload(Path outputFile, IndicativePayloadOptions options) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        Objects.requireNonNull(options, "options must not be null");
        Files.createDirectories(outputFile.toAbsolutePath().getParent());

        Random random = new Random(options.seed());
        Faker faker = new Faker(Locale.US, new Random(options.seed()));
        ScenarioTally tally = new ScenarioTally();

        try (OutputStream output = Files.newOutputStream(outputFile);
             JsonGenerator json = JsonUtil.objectMapper().getFactory().createGenerator(output)) {
            json.useDefaultPrettyPrinter();
            json.writeStartObject();
            json.writeStringField("correlationId", deterministicUuid(options.seed(), "indicative-correlation"));
            json.writeStringField("batchId", "batch-prismhr-ind-" + DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.of(2026, 5, 22)));
            json.writeStringField("vendorKey", "PRISMHR");
            json.writeStringField("planId", "PEARL-401K-PLAN-001");
            json.writeStringField("processingTimestamp", PROCESSING_TIMESTAMP.toString());
            json.writeNumberField("entityCount", options.entityCount());
            writeIndicativeLoadMetadata(json, options);

            json.writeArrayFieldStart("entities");
            for (int index = 1; index <= options.entityCount(); index++) {
                writeEntity(json, faker, random, options, index, tally);
            }
            json.writeEndArray();

            writeScenarioSummary(json, tally);
            json.writeEndObject();
        }

        long bytes = Files.size(outputFile);
        return new PayloadGenerationMetrics(
                outputFile,
                "INDICATIVE",
                bytes,
                options.entityCount(),
                0,
                500,
                recommendedS3Parts(bytes),
                "Split indicative payloads into 500-entity chunks; route Step Functions map states by S3 object pointer.");
    }

    public List<PayloadGenerationMetrics> writeScenarioPayloads(Path scenarioDirectory) throws IOException {
        Files.createDirectories(scenarioDirectory);
        List<PayloadGenerationMetrics> metrics = new ArrayList<>();
        metrics.add(writePayload(scenarioDirectory.resolve("indicative-valid-small.json"),
                IndicativePayloadOptions.small(24101L).withoutInjectedErrors()));
        metrics.add(writePayload(scenarioDirectory.resolve("indicative-malformed-records.json"),
                IndicativePayloadOptions.small(24102L).withRates(0.34, 0.08, 0.06, 0.12)));
        metrics.add(writePayload(scenarioDirectory.resolve("indicative-retry-scenarios.json"),
                IndicativePayloadOptions.small(24103L).withRates(0.03, 0.04, 0.42, 0.03)));
        metrics.add(writePayload(scenarioDirectory.resolve("indicative-partial-failure.json"),
                IndicativePayloadOptions.small(24104L).withRates(0.08, 0.38, 0.12, 0.05)));
        return metrics;
    }

    private void writeEntity(
            JsonGenerator json,
            Faker faker,
            Random random,
            IndicativePayloadOptions options,
            int index,
            ScenarioTally tally) throws IOException {
        Scenario scenario = scenarioFor(options, random, index);
        tally.add(scenario);

        String participantId = scenario.duplicateParticipant()
                ? "P" + String.format(Locale.ROOT, "%010d", 990000000 + Math.max(1, index - 1))
                : "P" + String.format(Locale.ROOT, "%010d", 990000000 + index);
        String employeeId = "E" + String.format(Locale.ROOT, "%08d", 700000 + index);
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        String maritalStatus = pick(random, MARITAL_STATUSES);
        String employmentStatus = scenario.terminated() ? "TERMINATED" : pick(random, EMPLOYMENT_STATUSES);
        LocalDate hireDate = randomPastDate(random, 1999, 2026);
        LocalDate dateOfBirth = randomBirthDate(random);
        String state = pick(random, STATES);

        json.writeStartObject();
        json.writeObjectFieldStart("identity");
        json.writeStringField("participantId", participantId);
        writeNullableString(json, "employeeId", scenario.partial() && index % 3 == 0 ? null : employeeId);
        json.writeStringField("ssnMasked", scenario.malformed() && index % 2 == 0 ? "999999999" : maskedSsn(random));
        json.writeStringField("firstName", firstName);
        writeNullableString(json, "middleName", random.nextDouble() < 0.62D ? faker.name().firstName() : null);
        json.writeStringField("lastName", lastName);
        writeNullableString(json, "suffix", random.nextDouble() < 0.08D ? pick(random, new String[]{"Jr.", "Sr.", "II", "III"}) : null);
        json.writeEndObject();

        json.writeObjectFieldStart("personalDetails");
        json.writeStringField("gender", pick(random, new String[]{"FEMALE", "MALE", "NON_BINARY", "UNDISCLOSED"}));
        json.writeStringField("maritalStatus", maritalStatus);
        json.writeStringField("dateOfBirth", dateOfBirth.toString());
        json.writeStringField("preferredLanguage", pick(random, LANGUAGES));
        json.writeStringField("citizenshipStatus", pick(random, CITIZENSHIP));
        json.writeEndObject();

        json.writeObjectFieldStart("contactDetails");
        String personalEmail = faker.internet().emailAddress();
        writeNullableString(json, "personalEmail", scenario.malformed() && index % 2 == 1 ? "bad-email-at-domain" : personalEmail);
        json.writeStringField("workEmail", employeeId.toLowerCase(Locale.ROOT) + "@client" + (100 + (index % 17)) + ".example.com");
        writeNullableString(json, "mobilePhone", scenario.partial() && index % 2 == 0 ? null : phone(random));
        writeNullableString(json, "homePhone", random.nextDouble() < 0.38D || scenario.partial() ? null : phone(random));
        json.writeEndObject();

        json.writeObjectFieldStart("addresses");
        writeAddress(json, "homeAddress", faker, random, state, scenario.invalidAddress());
        if (random.nextDouble() < 0.74D && !scenario.invalidAddress()) {
            writeAddress(json, "mailingAddress", faker, random, state, false);
        } else {
            writeAddress(json, "mailingAddress", faker, random, pick(random, STATES), scenario.invalidAddress() && index % 2 == 0);
        }
        json.writeEndObject();

        json.writeObjectFieldStart("employmentDetails");
        json.writeStringField("hireDate", hireDate.toString());
        writeNullableString(json, "terminationDate", "TERMINATED".equals(employmentStatus) ? randomPastDate(random, 2024, 2026).toString() : null);
        writeNullableString(json, "employmentStatus", scenario.partial() && index % 5 == 0 ? null : employmentStatus);
        json.writeStringField("employmentType", pick(random, EMPLOYMENT_TYPES));
        json.writeStringField("unionStatus", random.nextDouble() < 0.18D ? "UNION" : "NON_UNION");
        json.writeStringField("compensationType", pick(random, COMPENSATION_TYPES));
        writeMoney(json, "annualizedCompensation", compensation(random, employmentStatus));
        json.writeStringField("departmentCode", "D" + String.format(Locale.ROOT, "%04d", 100 + (index % 850)));
        json.writeStringField("locationCode", "LOC-" + state + "-" + String.format(Locale.ROOT, "%03d", 1 + (index % 130)));
        json.writeEndObject();

        json.writeObjectFieldStart("benefits");
        json.writeStringField("medicalPlan", pick(random, MEDICAL_PLANS));
        json.writeStringField("dentalPlan", pick(random, DENTAL_PLANS));
        json.writeStringField("visionPlan", pick(random, VISION_PLANS));
        json.writeStringField("disabilityPlan", pick(random, DISABILITY_PLANS));
        json.writeStringField("lifeInsurance", pick(random, LIFE_PLANS));
        json.writeBooleanField("hsaEligible", random.nextDouble() < 0.36D);
        json.writeBooleanField("benefitsWaived", random.nextDouble() < 0.07D);
        json.writeEndObject();

        writeDependents(json, faker, random, maritalStatus, lastName, dateOfBirth);
        writeRetirementDetails(json, random, hireDate, employmentStatus);
        writeAuditMetadata(json, index);
        writeRecordScenario(json, scenario, index);
        json.writeEndObject();
    }

    private void writeDependents(JsonGenerator json, Faker faker, Random random, String maritalStatus, String lastName, LocalDate dateOfBirth) throws IOException {
        json.writeObjectFieldStart("dependents");
        boolean married = maritalStatus.equals("MARRIED") || maritalStatus.equals("DOMESTIC_PARTNER");
        json.writeFieldName("spouse");
        if (married && random.nextDouble() < 0.82D) {
            json.writeStartObject();
            json.writeStringField("dependentId", "DEP-S-" + deterministicUuid(random.nextLong(), lastName).substring(0, 8));
            json.writeStringField("firstName", faker.name().firstName());
            json.writeStringField("lastName", lastName);
            json.writeStringField("relationship", maritalStatus.equals("DOMESTIC_PARTNER") ? "DOMESTIC_PARTNER" : "SPOUSE");
            json.writeStringField("dateOfBirth", dateOfBirth.plusYears(1 + random.nextInt(8)).toString());
            json.writeBooleanField("medicalCovered", random.nextDouble() < 0.74D);
            json.writeBooleanField("dentalCovered", random.nextDouble() < 0.71D);
            json.writeEndObject();
        } else {
            json.writeNull();
        }

        int childCount = random.nextDouble() < 0.48D ? random.nextInt(4) : 0;
        json.writeArrayFieldStart("children");
        for (int child = 1; child <= childCount; child++) {
            json.writeStartObject();
            json.writeStringField("dependentId", "DEP-C-" + deterministicUuid(random.nextLong(), lastName + child).substring(0, 8));
            json.writeStringField("firstName", faker.name().firstName());
            json.writeStringField("lastName", lastName);
            json.writeStringField("relationship", "CHILD");
            json.writeStringField("dateOfBirth", LocalDate.of(2008 + random.nextInt(18), 1 + random.nextInt(12), 1 + random.nextInt(26)).toString());
            json.writeBooleanField("studentStatus", random.nextDouble() < 0.22D);
            json.writeBooleanField("disabledDependent", random.nextDouble() < 0.03D);
            json.writeBooleanField("medicalCovered", random.nextDouble() < 0.69D);
            json.writeEndObject();
        }
        json.writeEndArray();

        int beneficiaryCount = 1 + random.nextInt(3);
        BigDecimal remainingPercentage = BigDecimal.valueOf(100);
        json.writeArrayFieldStart("beneficiaries");
        for (int beneficiary = 1; beneficiary <= beneficiaryCount; beneficiary++) {
            BigDecimal percentage = beneficiary == beneficiaryCount
                    ? remainingPercentage
                    : BigDecimal.valueOf(10 + random.nextInt(50)).min(remainingPercentage).setScale(2, RoundingMode.HALF_UP);
            remainingPercentage = remainingPercentage.subtract(percentage).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            json.writeStartObject();
            json.writeStringField("beneficiaryId", "BEN-" + deterministicUuid(random.nextLong(), lastName + beneficiary).substring(0, 10));
            json.writeStringField("firstName", faker.name().firstName());
            json.writeStringField("lastName", random.nextDouble() < 0.55D ? lastName : faker.name().lastName());
            json.writeStringField("relationship", pick(random, BENEFICIARY_RELATIONSHIPS));
            json.writeFieldName("allocationPercentage");
            json.writeNumber(percentage);
            json.writeStringField("beneficiaryType", beneficiary == 1 ? "PRIMARY" : (random.nextDouble() < 0.65D ? "PRIMARY" : "CONTINGENT"));
            json.writeEndObject();
        }
        json.writeEndArray();
        json.writeEndObject();
    }

    private void writeAddress(JsonGenerator json, String field, Faker faker, Random random, String state, boolean invalid) throws IOException {
        json.writeObjectFieldStart(field);
        writeNullableString(json, "line1", invalid ? null : faker.address().streetAddress());
        writeNullableString(json, "line2", random.nextDouble() < 0.22D ? "Apt " + (100 + random.nextInt(900)) : null);
        json.writeStringField("city", invalid ? "" : faker.address().cityName());
        json.writeStringField("state", invalid ? "ZZ" : state);
        json.writeStringField("zipCode", invalid ? "000" : String.format(Locale.ROOT, "%05d", 10_000 + random.nextInt(89_000)));
        json.writeStringField("country", "US");
        json.writeBooleanField("addressValidated", !invalid && random.nextDouble() < 0.88D);
        json.writeStringField("county", invalid ? "UNKNOWN" : faker.address().cityName() + " County");
        json.writeEndObject();
    }

    private void writeRetirementDetails(JsonGenerator json, Random random, LocalDate hireDate, String employmentStatus) throws IOException {
        LocalDate eligibilityDate = hireDate.plusDays(30 + random.nextInt(75));
        boolean autoEnrollment = random.nextDouble() < 0.71D;
        json.writeObjectFieldStart("retirementDetails");
        json.writeStringField("vestingStatus", random.nextDouble() < 0.77D ? "VESTED" : "NOT_FULLY_VESTED");
        json.writeStringField("eligibilityDate", eligibilityDate.toString());
        writeNullableString(json, "enrollmentDate", autoEnrollment || random.nextDouble() < 0.78D ? eligibilityDate.plusDays(random.nextInt(45)).toString() : null);
        json.writeBooleanField("autoEnrollmentFlag", autoEnrollment);
        json.writeStringField("deferralStatus", "TERMINATED".equals(employmentStatus) ? "STOPPED" : pick(random, new String[]{"ACTIVE", "ACTIVE", "PENDING", "OPTED_OUT"}));
        json.writeBooleanField("catchUpEligible", random.nextDouble() < 0.18D);
        json.writeEndObject();
    }

    private void writeAuditMetadata(JsonGenerator json, int index) throws IOException {
        json.writeObjectFieldStart("auditMetadata");
        json.writeStringField("sourceSystem", "PRISMHR");
        json.writeStringField("sourceTimestamp", PROCESSING_TIMESTAMP.minusSeconds(index * 2L).toString());
        json.writeStringField("ingestionTimestamp", PROCESSING_TIMESTAMP.plusMillis(index * 97L).toString());
        json.writeStringField("schemaVersion", "2026.05.24-indicative-v1");
        json.writeStringField("sourceFileName", "PRISMHR_IND_PARTICIPANTS_2026-05-22.json");
        json.writeEndObject();
    }

    private void writeRecordScenario(JsonGenerator json, Scenario scenario, int index) throws IOException {
        json.writeObjectFieldStart("scenarioFlags");
        json.writeBooleanField("malformedRecord", scenario.malformed());
        json.writeBooleanField("partialRecord", scenario.partial());
        json.writeBooleanField("retryScenario", scenario.retry());
        json.writeBooleanField("duplicateParticipantId", scenario.duplicateParticipant());
        json.writeBooleanField("invalidAddress", scenario.invalidAddress());
        json.writeArrayFieldStart("edgeCaseTypes");
        for (String edgeType : scenario.edgeTypes()) {
            json.writeString(edgeType);
        }
        json.writeEndArray();
        json.writeEndObject();

        if (scenario.retry()) {
            json.writeObjectFieldStart("retrySimulation");
            json.writeBooleanField("retryable", true);
            json.writeStringField("errorCode", "DYNAMODB_CONDITIONAL_WRITE_CONFLICT");
            json.writeStringField("lastFailureReason", "Simulated participant write conflict during idempotent upsert");
            json.writeNumberField("attemptCount", 1 + (index % 4));
            json.writeStringField("retryAfter", PROCESSING_TIMESTAMP.plusSeconds(90L + index).toString());
            json.writeStringField("idempotencyKey", "indicative-entity-" + String.format(Locale.ROOT, "%05d", index));
            json.writeEndObject();
        } else {
            json.writeNullField("retrySimulation");
        }
    }

    private Scenario scenarioFor(IndicativePayloadOptions options, Random random, int index) {
        boolean malformed = random.nextDouble() < options.malformedRecordRate();
        boolean partial = random.nextDouble() < options.partialRecordRate();
        boolean retry = random.nextDouble() < options.retryScenarioRate();
        boolean duplicate = random.nextDouble() < options.duplicateParticipantRate();
        boolean invalidAddress = false;
        boolean terminated = false;
        List<String> edgeTypes = new ArrayList<>();
        if (options.includeDeterministicEdgeCases()) {
            if (index % 811 == 0) {
                duplicate = true;
                edgeTypes.add("DUPLICATE_PARTICIPANT_ID");
            }
            if (index % 677 == 0) {
                malformed = true;
                edgeTypes.add("MALFORMED_EMAIL");
            }
            if (index % 509 == 0) {
                partial = true;
                edgeTypes.add("MISSING_PHONE_NUMBER");
            }
            if (index % 421 == 0) {
                invalidAddress = true;
                edgeTypes.add("INVALID_ADDRESS");
            }
            if (index % 367 == 0) {
                retry = true;
                edgeTypes.add("SIMULATED_DYNAMODB_CONFLICT");
            }
            if (index % 307 == 0) {
                terminated = true;
                edgeTypes.add("TERMINATED_PARTICIPANT");
            }
        }
        if (malformed && !edgeTypes.contains("MALFORMED_EMAIL")) {
            edgeTypes.add("MALFORMED_INDICATIVE_RECORD");
        }
        if (partial && !edgeTypes.contains("MISSING_PHONE_NUMBER")) {
            edgeTypes.add("PARTIAL_INDICATIVE_RECORD");
        }
        if (retry && !edgeTypes.contains("SIMULATED_DYNAMODB_CONFLICT")) {
            edgeTypes.add("RETRYABLE_DYNAMODB_WRITE");
        }
        if (duplicate && !edgeTypes.contains("DUPLICATE_PARTICIPANT_ID")) {
            edgeTypes.add("DUPLICATE_PARTICIPANT_ID");
        }
        return new Scenario(malformed, partial, retry, duplicate, invalidAddress, terminated, edgeTypes);
    }

    private void writeIndicativeLoadMetadata(JsonGenerator json, IndicativePayloadOptions options) throws IOException {
        json.writeObjectFieldStart("loadTestMetadata");
        json.writeStringField("dataset", "PrismHR-style participant demographic and benefits extract");
        json.writeNumberField("expectedEntityCount", options.entityCount());
        json.writeNumberField("recommendedEntityChunkSize", 500);
        json.writeNumberField("recommendedStepFunctionMapConcurrency", 10);
        json.writeStringField("sqsMessageMode", "S3_POINTER_PER_500_ENTITIES");
        json.writeStringField("chunkingRecommendation", "Chunk at 500 entities for local tests; reduce to 250 for high-beneficiary populations.");
        json.writeEndObject();
    }

    private void writeScenarioSummary(JsonGenerator json, ScenarioTally tally) throws IOException {
        json.writeObjectFieldStart("scenarioSummary");
        json.writeNumberField("malformedRecords", tally.malformed);
        json.writeNumberField("partialRecords", tally.partial);
        json.writeNumberField("retryScenarios", tally.retry);
        json.writeNumberField("duplicateParticipantIds", tally.duplicates);
        json.writeNumberField("invalidAddresses", tally.invalidAddresses);
        json.writeNumberField("terminatedParticipantRecords", tally.terminated);
        json.writeEndObject();
    }

    private static String deterministicUuid(long seed, String value) {
        return UUID.nameUUIDFromBytes((seed + ":" + value).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private static String maskedSsn(Random random) {
        return "***-**-" + String.format(Locale.ROOT, "%04d", random.nextInt(10_000));
    }

    private static String phone(Random random) {
        return "+1-" + (200 + random.nextInt(700)) + "-" + (200 + random.nextInt(700)) + "-" + String.format(Locale.ROOT, "%04d", random.nextInt(10_000));
    }

    private static LocalDate randomPastDate(Random random, int startYear, int endYear) {
        int year = startYear + random.nextInt(Math.max(1, endYear - startYear + 1));
        int day = 1 + random.nextInt(LocalDate.of(year, 12, 31).getDayOfYear());
        return LocalDate.ofYearDay(year, day);
    }

    private static LocalDate randomBirthDate(Random random) {
        int age = 18 + random.nextInt(58);
        LocalDate base = LocalDate.of(2026, 5, 22).minusYears(age);
        return base.minusDays(random.nextInt(365));
    }

    private static BigDecimal compensation(Random random, String employmentStatus) {
        if ("TERMINATED".equals(employmentStatus) || "RETIRED".equals(employmentStatus)) {
            return BigDecimal.ZERO.setScale(2);
        }
        double value = random.nextDouble() < 0.05D
                ? 160_000D + random.nextDouble() * 390_000D
                : 34_000D + random.nextDouble() * 126_000D;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static void writeMoney(JsonGenerator json, String field, BigDecimal value) throws IOException {
        json.writeFieldName(field);
        json.writeNumber(value);
    }

    private static void writeNullableString(JsonGenerator json, String field, String value) throws IOException {
        if (value == null) {
            json.writeNullField(field);
        } else {
            json.writeStringField(field, value);
        }
    }

    private static String pick(Random random, String[] values) {
        return values[random.nextInt(values.length)];
    }

    private static int recommendedS3Parts(long bytes) {
        long fiveMb = 5L * 1024L * 1024L;
        return Math.max(1, (int) Math.ceil((double) bytes / fiveMb));
    }

    public record IndicativePayloadOptions(
            long seed,
            int entityCount,
            double malformedRecordRate,
            double partialRecordRate,
            double retryScenarioRate,
            double duplicateParticipantRate,
            boolean includeDeterministicEdgeCases) {

        public IndicativePayloadOptions {
            if (entityCount <= 0) {
                throw new IllegalArgumentException("entityCount must be positive");
            }
            validateRate("malformedRecordRate", malformedRecordRate);
            validateRate("partialRecordRate", partialRecordRate);
            validateRate("retryScenarioRate", retryScenarioRate);
            validateRate("duplicateParticipantRate", duplicateParticipantRate);
        }

        public static IndicativePayloadOptions large() {
            return new IndicativePayloadOptions(51001L, 5_000, 0.006D, 0.009D, 0.010D, 0.004D, true);
        }

        public static IndicativePayloadOptions small(long seed) {
            return new IndicativePayloadOptions(seed, 50, 0.04D, 0.05D, 0.05D, 0.04D, true);
        }

        public IndicativePayloadOptions withRates(double malformedRate, double partialRate, double retryRate, double duplicateRate) {
            return new IndicativePayloadOptions(seed, entityCount, malformedRate, partialRate, retryRate, duplicateRate,
                    includeDeterministicEdgeCases);
        }

        public IndicativePayloadOptions withoutInjectedErrors() {
            return new IndicativePayloadOptions(seed, entityCount, 0.0D, 0.0D, 0.0D, 0.0D, false);
        }

        public IndicativePayloadOptions withEntityCount(int newEntityCount) {
            return new IndicativePayloadOptions(seed, newEntityCount, malformedRecordRate, partialRecordRate,
                    retryScenarioRate, duplicateParticipantRate, includeDeterministicEdgeCases);
        }

        private static void validateRate(String name, double rate) {
            if (rate < 0.0D || rate > 1.0D) {
                throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
            }
        }
    }

    private record Scenario(
            boolean malformed,
            boolean partial,
            boolean retry,
            boolean duplicateParticipant,
            boolean invalidAddress,
            boolean terminated,
            List<String> edgeTypes) {
    }

    private static final class ScenarioTally {
        private int malformed;
        private int partial;
        private int retry;
        private int duplicates;
        private int invalidAddresses;
        private int terminated;

        private void add(Scenario scenario) {
            if (scenario.malformed()) {
                malformed++;
            }
            if (scenario.partial()) {
                partial++;
            }
            if (scenario.retry()) {
                retry++;
            }
            if (scenario.duplicateParticipant()) {
                duplicates++;
            }
            if (scenario.invalidAddress()) {
                invalidAddresses++;
            }
            if (scenario.terminated()) {
                terminated++;
            }
        }
    }
}
