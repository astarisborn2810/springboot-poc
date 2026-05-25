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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.datafaker.Faker;

public final class FinancialPayloadGenerator {

    private static final Instant PROCESSING_TIMESTAMP = Instant.parse("2026-05-22T23:45:30Z");
    private static final LocalDate PAYROLL_DATE = LocalDate.of(2026, 5, 22);
    private static final LocalDate PAY_PERIOD_START = LocalDate.of(2026, 5, 9);
    private static final LocalDate PAY_PERIOD_END = LocalDate.of(2026, 5, 22);
    private static final String[] STATUSES = {"ACTIVE", "ACTIVE", "ACTIVE", "ACTIVE", "ACTIVE", "LEAVE", "TERMINATED"};
    private static final String[] DEPARTMENTS = {
            "Payroll Operations", "Manufacturing", "Distribution", "Retail Operations", "Client Success",
            "Engineering", "Finance", "Human Resources", "Benefits Administration", "Field Services"
    };
    private static final String[] DIVISIONS = {
            "North America", "Enterprise Accounts", "Shared Services", "Central Operations", "West Region",
            "East Region", "Retirement Services", "Benefits Center"
    };
    private static final String[] LOCATIONS = {
            "NJ-JERSEY-CITY", "TX-DALLAS", "GA-ATLANTA", "IL-CHICAGO", "CA-IRVINE",
            "FL-TAMPA", "AZ-PHOENIX", "OH-COLUMBUS", "REMOTE-US", "MA-BOSTON"
    };
    private static final String[] PAYROLL_GROUPS = {"BIWEEKLY-A", "BIWEEKLY-B", "WEEKLY-OPS", "SEMI-MONTHLY", "EXEC"};
    private static final String[] BUSINESS_UNITS = {
            "PEO-ENTERPRISE", "PAYROLL-HCM", "RETIREMENT-PLAN", "CLIENT-OPS", "CORPORATE"
    };

    public PayloadGenerationMetrics writePayload(Path outputFile, FinancialPayloadOptions options) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        Objects.requireNonNull(options, "options must not be null");
        Files.createDirectories(outputFile.toAbsolutePath().getParent());

        Random random = new Random(options.seed());
        Faker faker = new Faker(Locale.US, new Random(options.seed()));
        ScenarioTally tally = new ScenarioTally();
        int totalRecords = options.totalMembers();

        try (OutputStream output = Files.newOutputStream(outputFile);
             JsonGenerator json = JsonUtil.objectMapper().getFactory().createGenerator(output)) {
            json.useDefaultPrettyPrinter();
            json.writeStartObject();
            json.writeStringField("correlationId", deterministicUuid(options.seed(), "financial-correlation"));
            json.writeStringField("batchId", "batch-prismhr-fin-" + DateTimeFormatter.BASIC_ISO_DATE.format(PAYROLL_DATE));
            json.writeStringField("vendorKey", "PRISMHR");
            json.writeStringField("planId", "PEARL-401K-PLAN-001");
            json.writeStringField("payrollDate", PAYROLL_DATE.toString());
            json.writeStringField("payPeriodStart", PAY_PERIOD_START.toString());
            json.writeStringField("payPeriodEnd", PAY_PERIOD_END.toString());
            json.writeStringField("processingTimestamp", PROCESSING_TIMESTAMP.toString());
            json.writeNumberField("totalPages", options.pageCount());
            json.writeNumberField("totalMembers", totalRecords);
            writeFinancialLoadMetadata(json, options);

            json.writeArrayFieldStart("pages");
            for (int page = 1; page <= options.pageCount(); page++) {
                writePage(json, faker, random, options, page, tally);
            }
            json.writeEndArray();

            writeScenarioSummary(json, tally);
            json.writeEndObject();
        }

        long bytes = Files.size(outputFile);
        return new PayloadGenerationMetrics(
                outputFile,
                "FINANCIAL",
                bytes,
                totalRecords,
                options.pageCount(),
                250,
                recommendedS3Parts(bytes),
                "Keep PrismHR financial pages at 250 members; fan out ECS work by page and split SQS messages to S3 pointers.");
    }

    public List<PayloadGenerationMetrics> writeScenarioPayloads(Path scenarioDirectory) throws IOException {
        Files.createDirectories(scenarioDirectory);
        List<PayloadGenerationMetrics> metrics = new ArrayList<>();
        metrics.add(writePayload(scenarioDirectory.resolve("financial-valid-small.json"),
                FinancialPayloadOptions.small(14101L).withoutInjectedErrors()));
        metrics.add(writePayload(scenarioDirectory.resolve("financial-malformed-records.json"),
                FinancialPayloadOptions.small(14102L).withRates(0.35, 0.05, 0.05)));
        metrics.add(writePayload(scenarioDirectory.resolve("financial-retry-scenarios.json"),
                FinancialPayloadOptions.small(14103L).withRates(0.02, 0.03, 0.45)));
        metrics.add(writePayload(scenarioDirectory.resolve("financial-partial-failure.json"),
                FinancialPayloadOptions.small(14104L).withRates(0.08, 0.40, 0.10)));
        return metrics;
    }

    private void writePage(
            JsonGenerator json,
            Faker faker,
            Random random,
            FinancialPayloadOptions options,
            int page,
            ScenarioTally tally) throws IOException {
        int start = ((page - 1) * options.membersPerPage()) + 1;
        int end = start + options.membersPerPage() - 1;
        json.writeStartObject();
        json.writeStringField("pageId", pageId(page));
        json.writeNumberField("pageNumber", page);
        json.writeNumberField("recordCount", options.membersPerPage());
        json.writeStringField("parentPageReference", "financial-payload-large");
        writeNullableString(json, "previousPageReference", page == 1 ? null : pageId(page - 1));
        writeNullableString(json, "nextPageReference", page == options.pageCount() ? null : pageId(page + 1));
        json.writeStringField("pageCreatedTimestamp", PROCESSING_TIMESTAMP.plusSeconds(page * 3L).toString());
        json.writeStringField("pageChecksum", "sha256-simulated-fin-" + deterministicUuid(options.seed(), "page-" + page).replace("-", ""));
        json.writeObjectFieldStart("paginationMetadata");
        json.writeNumberField("pageSize", options.membersPerPage());
        json.writeNumberField("memberOrdinalStart", start);
        json.writeNumberField("memberOrdinalEnd", end);
        json.writeBooleanField("firstPage", page == 1);
        json.writeBooleanField("lastPage", page == options.pageCount());
        json.writeEndObject();

        writeSubPages(json, page, start, options.membersPerPage());

        json.writeArrayFieldStart("members");
        for (int memberIndex = 1; memberIndex <= options.membersPerPage(); memberIndex++) {
            int globalIndex = start + memberIndex - 1;
            writeMember(json, faker, random, options, page, memberIndex, globalIndex, tally);
        }
        json.writeEndArray();
        json.writeEndObject();
    }

    private void writeSubPages(JsonGenerator json, int page, int start, int pageSize) throws IOException {
        int subPageSize = Math.max(25, pageSize / 5);
        int subPageCount = (int) Math.ceil((double) pageSize / subPageSize);
        json.writeNumberField("subPageCount", subPageCount);
        json.writeArrayFieldStart("subPages");
        for (int subPage = 1; subPage <= subPageCount; subPage++) {
            int subStart = start + ((subPage - 1) * subPageSize);
            int subEnd = Math.min(start + pageSize - 1, subStart + subPageSize - 1);
            json.writeStartObject();
            json.writeStringField("subPageId", pageId(page) + "-sub-" + String.format(Locale.ROOT, "%02d", subPage));
            json.writeStringField("parentPageId", pageId(page));
            json.writeNumberField("subPageNumber", subPage);
            json.writeNumberField("memberOrdinalStart", subStart);
            json.writeNumberField("memberOrdinalEnd", subEnd);
            json.writeNumberField("recordCount", subEnd - subStart + 1);
            json.writeStringField("recommendedS3Key",
                    "payloads/financial/pages/" + pageId(page) + "/sub-" + String.format(Locale.ROOT, "%02d", subPage) + ".json");
            json.writeEndObject();
        }
        json.writeEndArray();
    }

    private void writeMember(
            JsonGenerator json,
            Faker faker,
            Random random,
            FinancialPayloadOptions options,
            int page,
            int memberIndex,
            int globalIndex,
            ScenarioTally tally) throws IOException {
        Scenario scenario = scenarioFor(options, random, globalIndex);
        tally.add(scenario);
        boolean terminated = scenario.terminated() || random.nextDouble() < 0.07D;
        boolean highEarner = scenario.highEarner() || random.nextDouble() < 0.03D;
        boolean loanRepayment = scenario.loanRepayment() || random.nextDouble() < 0.12D;
        String employeeId = "E" + String.format(Locale.ROOT, "%08d", 700000 + globalIndex);
        String participantId = "P" + String.format(Locale.ROOT, "%010d", 990000000 + globalIndex);
        BigDecimal grossPay = highEarner ? money(random, 9_500, 37_500) : money(random, 1_850, 8_900);
        if (scenario.malformed() && globalIndex % 2 == 0) {
            grossPay = money(random, -250, -1);
        }
        BigDecimal overtimePay = random.nextDouble() < 0.28D ? money(random, 75, 1_450) : BigDecimal.ZERO.setScale(2);
        BigDecimal bonusPay = random.nextDouble() < 0.11D ? money(random, 250, highEarner ? 15_000 : 3_500) : BigDecimal.ZERO.setScale(2);
        BigDecimal commissionPay = random.nextDouble() < 0.08D ? money(random, 150, 5_500) : BigDecimal.ZERO.setScale(2);
        BigDecimal holidayPay = random.nextDouble() < 0.14D ? money(random, 80, 700) : BigDecimal.ZERO.setScale(2);
        BigDecimal totalCompensation = grossPay.add(overtimePay).add(bonusPay).add(commissionPay).add(holidayPay);
        BigDecimal federalTax = percent(totalCompensation, random, 0.10D, highEarner ? 0.29D : 0.22D);
        BigDecimal stateTax = percent(totalCompensation, random, 0.02D, 0.09D);
        BigDecimal medicareTax = totalCompensation.multiply(BigDecimal.valueOf(0.0145D)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal socialSecurityTax = totalCompensation.multiply(BigDecimal.valueOf(0.062D)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal preTax401k = percent(grossPay.max(BigDecimal.ZERO), random, 0.00D, highEarner ? 0.14D : 0.09D);
        BigDecimal roth401k = random.nextDouble() < 0.36D ? percent(grossPay.max(BigDecimal.ZERO), random, 0.01D, 0.06D) : BigDecimal.ZERO.setScale(2);
        BigDecimal afterTaxContribution = random.nextDouble() < 0.07D ? percent(grossPay.max(BigDecimal.ZERO), random, 0.01D, 0.05D) : BigDecimal.ZERO.setScale(2);
        BigDecimal employerMatch = preTax401k.add(roth401k).multiply(BigDecimal.valueOf(0.45D)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profitSharing = random.nextDouble() < 0.04D ? money(random, 100, highEarner ? 4_500 : 1_200) : BigDecimal.ZERO.setScale(2);
        BigDecimal safeHarbor = random.nextDouble() < 0.33D ? grossPay.max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(0.03D)).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        BigDecimal loan = loanRepayment ? money(random, 25, 525) : BigDecimal.ZERO.setScale(2);
        BigDecimal medical = random.nextDouble() < 0.82D ? money(random, 70, 360) : BigDecimal.ZERO.setScale(2);
        BigDecimal dental = random.nextDouble() < 0.72D ? money(random, 8, 48) : BigDecimal.ZERO.setScale(2);
        BigDecimal vision = random.nextDouble() < 0.68D ? money(random, 3, 20) : BigDecimal.ZERO.setScale(2);
        BigDecimal garnishments = random.nextDouble() < 0.018D ? money(random, 45, 650) : BigDecimal.ZERO.setScale(2);
        BigDecimal hsa = random.nextDouble() < 0.23D ? money(random, 15, 215) : BigDecimal.ZERO.setScale(2);
        BigDecimal fsa = random.nextDouble() < 0.17D ? money(random, 10, 190) : BigDecimal.ZERO.setScale(2);
        BigDecimal totalDeductions = federalTax.add(stateTax).add(medicareTax).add(socialSecurityTax)
                .add(preTax401k).add(roth401k).add(afterTaxContribution).add(loan)
                .add(medical).add(dental).add(vision).add(garnishments).add(hsa).add(fsa);
        BigDecimal netPay = totalCompensation.subtract(totalDeductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        json.writeStartObject();
        json.writeObjectFieldStart("identity");
        writeNullableString(json, "employeeId", scenario.partial() && globalIndex % 3 == 0 ? null : employeeId);
        writeNullableString(json, "participantId", participantId);
        json.writeStringField("ssnMasked", scenario.malformed() && globalIndex % 2 == 0 ? "INVALID-SSN" : maskedSsn(random));
        json.writeStringField("firstName", faker.name().firstName());
        json.writeStringField("lastName", faker.name().lastName());
        json.writeStringField("middleInitial", String.valueOf((char) ('A' + random.nextInt(26))));
        writeNullableString(json, "employmentStatus", scenario.partial() && globalIndex % 5 == 0 ? null : status(random, terminated));
        json.writeStringField("hireDate", randomPastDate(random, 2001, 2025).toString());
        writeNullableString(json, "terminationDate", terminated ? randomPastDate(random, 2024, 2026).toString() : null);
        json.writeEndObject();

        json.writeObjectFieldStart("payrollDetails");
        writeNullableMoney(json, "grossPay", scenario.partial() && globalIndex % 4 == 0 ? null : grossPay);
        writeMoney(json, "netPay", netPay);
        writeMoney(json, "overtimePay", overtimePay);
        writeMoney(json, "bonusPay", bonusPay);
        writeMoney(json, "commissionPay", commissionPay);
        writeMoney(json, "holidayPay", holidayPay);
        json.writeStringField("payFrequency", random.nextDouble() < 0.81D ? "BIWEEKLY" : "WEEKLY");
        json.writeEndObject();

        json.writeObjectFieldStart("financialContributions");
        writeMoney(json, "preTax401k", preTax401k);
        writeMoney(json, "roth401k", roth401k);
        writeMoney(json, "afterTaxContribution", afterTaxContribution);
        writeMoney(json, "employerMatch", employerMatch);
        writeMoney(json, "profitSharing", profitSharing);
        writeMoney(json, "safeHarborContribution", safeHarbor);
        writeMoney(json, "loanRepayment", loan);
        json.writeEndObject();

        json.writeObjectFieldStart("taxes");
        writeMoney(json, "federalTax", federalTax);
        writeMoney(json, "stateTax", stateTax);
        writeMoney(json, "medicareTax", medicareTax);
        writeMoney(json, "socialSecurityTax", socialSecurityTax);
        json.writeEndObject();

        json.writeObjectFieldStart("deductions");
        writeMoney(json, "medicalDeduction", medical);
        writeMoney(json, "dentalDeduction", dental);
        writeMoney(json, "visionDeduction", vision);
        writeMoney(json, "garnishments", garnishments);
        writeMoney(json, "hsaContribution", hsa);
        writeMoney(json, "fsaContribution", fsa);
        json.writeEndObject();

        json.writeObjectFieldStart("employmentData");
        json.writeStringField("department", pick(random, DEPARTMENTS));
        json.writeStringField("division", pick(random, DIVISIONS));
        json.writeStringField("location", pick(random, LOCATIONS));
        json.writeStringField("payrollGroup", pick(random, PAYROLL_GROUPS));
        json.writeStringField("businessUnit", pick(random, BUSINESS_UNITS));
        json.writeEndObject();

        json.writeObjectFieldStart("audit");
        json.writeStringField("recordCreatedTimestamp", PROCESSING_TIMESTAMP.plusMillis(globalIndex * 137L).toString());
        json.writeStringField("sourceSystem", "PRISMHR");
        json.writeStringField("sourceFileName", "PRISMHR_FIN_" + PAYROLL_DATE + "_PAGE_" + String.format(Locale.ROOT, "%03d", page) + ".json");
        json.writeStringField("version", "2026.05.24-financial-v1");
        json.writeEndObject();

        writeRecordScenario(json, scenario, page, memberIndex, globalIndex);
        json.writeEndObject();
    }

    private Scenario scenarioFor(FinancialPayloadOptions options, Random random, int globalIndex) {
        boolean malformed = random.nextDouble() < options.malformedRecordRate();
        boolean partial = random.nextDouble() < options.partialRecordRate();
        boolean retry = random.nextDouble() < options.retryScenarioRate();
        boolean highEarner = false;
        boolean terminated = false;
        boolean loanRepayment = false;
        List<String> edgeTypes = new ArrayList<>();
        if (options.includeDeterministicEdgeCases()) {
            if (globalIndex % 997 == 0) {
                malformed = true;
                edgeTypes.add("NEGATIVE_GROSS_PAY");
            }
            if (globalIndex % 613 == 0) {
                partial = true;
                edgeTypes.add("MISSING_EMPLOYEE_ID");
            }
            if (globalIndex % 431 == 0) {
                retry = true;
                edgeTypes.add("SIMULATED_DOWNSTREAM_TIMEOUT");
            }
            if (globalIndex % 149 == 0) {
                highEarner = true;
                edgeTypes.add("HIGH_EARNER");
            }
            if (globalIndex % 353 == 0) {
                terminated = true;
                edgeTypes.add("TERMINATED_WITH_CURRENT_PAY");
            }
            if (globalIndex % 227 == 0) {
                loanRepayment = true;
                edgeTypes.add("ACTIVE_LOAN_REPAYMENT");
            }
        }
        if (malformed && !edgeTypes.contains("NEGATIVE_GROSS_PAY")) {
            edgeTypes.add("MALFORMED_FINANCIAL_RECORD");
        }
        if (partial && !edgeTypes.contains("MISSING_EMPLOYEE_ID")) {
            edgeTypes.add("PARTIAL_FINANCIAL_RECORD");
        }
        if (retry && !edgeTypes.contains("SIMULATED_DOWNSTREAM_TIMEOUT")) {
            edgeTypes.add("RETRYABLE_AWS_INTEGRATION_FAILURE");
        }
        return new Scenario(malformed, partial, retry, highEarner, terminated, loanRepayment, edgeTypes);
    }

    private void writeRecordScenario(JsonGenerator json, Scenario scenario, int page, int memberIndex, int globalIndex) throws IOException {
        json.writeObjectFieldStart("scenarioFlags");
        json.writeBooleanField("malformedRecord", scenario.malformed());
        json.writeBooleanField("partialRecord", scenario.partial());
        json.writeBooleanField("retryScenario", scenario.retry());
        json.writeBooleanField("highEarner", scenario.highEarner());
        json.writeBooleanField("loanRepaymentScenario", scenario.loanRepayment());
        json.writeArrayFieldStart("edgeCaseTypes");
        for (String edgeType : scenario.edgeTypes()) {
            json.writeString(edgeType);
        }
        json.writeEndArray();
        json.writeEndObject();

        if (scenario.retry()) {
            json.writeObjectFieldStart("retrySimulation");
            json.writeBooleanField("retryable", true);
            json.writeStringField("errorCode", "AWS_SQS_VISIBILITY_TIMEOUT");
            json.writeStringField("lastFailureReason", "Simulated transient downstream timeout for page worker");
            json.writeNumberField("attemptCount", 2 + (globalIndex % 3));
            json.writeStringField("retryAfter", PROCESSING_TIMESTAMP.plusSeconds(120L + globalIndex).toString());
            json.writeStringField("idempotencyKey", "financial-" + pageId(page) + "-" + String.format(Locale.ROOT, "%03d", memberIndex));
            json.writeEndObject();
        } else {
            json.writeNullField("retrySimulation");
        }
    }

    private void writeFinancialLoadMetadata(JsonGenerator json, FinancialPayloadOptions options) throws IOException {
        json.writeObjectFieldStart("loadTestMetadata");
        json.writeStringField("dataset", "PrismHR-style 401k retirement contribution payroll extract");
        json.writeNumberField("membersPerPage", options.membersPerPage());
        json.writeNumberField("expectedMemberCount", options.totalMembers());
        json.writeNumberField("recommendedEcsTasks", Math.max(2, Math.min(20, options.pageCount())));
        json.writeNumberField("recommendedSqsMessages", options.pageCount());
        json.writeStringField("sqsMessageMode", "S3_POINTER_PER_PAGE");
        json.writeStringField("chunkingRecommendation", "Process one page per ECS task; split subPages only when a page exceeds 8 MB uncompressed.");
        json.writeEndObject();
    }

    private void writeScenarioSummary(JsonGenerator json, ScenarioTally tally) throws IOException {
        json.writeObjectFieldStart("scenarioSummary");
        json.writeNumberField("malformedRecords", tally.malformed);
        json.writeNumberField("partialRecords", tally.partial);
        json.writeNumberField("retryScenarios", tally.retry);
        json.writeNumberField("highEarners", tally.highEarners);
        json.writeNumberField("loanRepaymentRecords", tally.loanRepayments);
        json.writeNumberField("terminatedEmployeeRecords", tally.terminated);
        json.writeEndObject();
    }

    private static String deterministicUuid(long seed, String value) {
        return UUID.nameUUIDFromBytes((seed + ":" + value).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private static String pageId(int page) {
        return "financial-page-" + String.format(Locale.ROOT, "%03d", page);
    }

    private static String status(Random random, boolean terminated) {
        if (terminated) {
            return "TERMINATED";
        }
        return pick(random, STATUSES);
    }

    private static String maskedSsn(Random random) {
        return "***-**-" + String.format(Locale.ROOT, "%04d", random.nextInt(10_000));
    }

    private static LocalDate randomPastDate(Random random, int startYear, int endYear) {
        int year = startYear + random.nextInt(Math.max(1, endYear - startYear + 1));
        int day = 1 + random.nextInt(LocalDate.of(year, 12, 31).getDayOfYear());
        return LocalDate.ofYearDay(year, day);
    }

    private static BigDecimal money(Random random, double minInclusive, double maxExclusive) {
        double value = minInclusive + (random.nextDouble() * (maxExclusive - minInclusive));
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal base, Random random, double min, double max) {
        return base.multiply(BigDecimal.valueOf(min + (random.nextDouble() * (max - min))))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void writeMoney(JsonGenerator json, String field, BigDecimal value) throws IOException {
        json.writeFieldName(field);
        json.writeNumber(value);
    }

    private static void writeNullableMoney(JsonGenerator json, String field, BigDecimal value) throws IOException {
        if (value == null) {
            json.writeNullField(field);
        } else {
            writeMoney(json, field, value);
        }
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

    public record FinancialPayloadOptions(
            long seed,
            int pageCount,
            int membersPerPage,
            double malformedRecordRate,
            double partialRecordRate,
            double retryScenarioRate,
            boolean includeDeterministicEdgeCases) {

        public FinancialPayloadOptions {
            if (pageCount <= 0) {
                throw new IllegalArgumentException("pageCount must be positive");
            }
            if (membersPerPage <= 0) {
                throw new IllegalArgumentException("membersPerPage must be positive");
            }
            validateRate("malformedRecordRate", malformedRecordRate);
            validateRate("partialRecordRate", partialRecordRate);
            validateRate("retryScenarioRate", retryScenarioRate);
        }

        public static FinancialPayloadOptions large() {
            return new FinancialPayloadOptions(41001L, 20, 250, 0.006D, 0.008D, 0.010D, true);
        }

        public static FinancialPayloadOptions small(long seed) {
            return new FinancialPayloadOptions(seed, 2, 25, 0.04D, 0.04D, 0.05D, true);
        }

        public int totalMembers() {
            return pageCount * membersPerPage;
        }

        public FinancialPayloadOptions withRates(double malformedRate, double partialRate, double retryRate) {
            return new FinancialPayloadOptions(seed, pageCount, membersPerPage, malformedRate, partialRate, retryRate, includeDeterministicEdgeCases);
        }

        public FinancialPayloadOptions withoutInjectedErrors() {
            return new FinancialPayloadOptions(seed, pageCount, membersPerPage, 0.0D, 0.0D, 0.0D, false);
        }

        public FinancialPayloadOptions withSize(int newPageCount, int newMembersPerPage) {
            return new FinancialPayloadOptions(seed, newPageCount, newMembersPerPage, malformedRecordRate,
                    partialRecordRate, retryScenarioRate, includeDeterministicEdgeCases);
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
            boolean highEarner,
            boolean terminated,
            boolean loanRepayment,
            List<String> edgeTypes) {
    }

    private static final class ScenarioTally {
        private int malformed;
        private int partial;
        private int retry;
        private int highEarners;
        private int loanRepayments;
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
            if (scenario.highEarner()) {
                highEarners++;
            }
            if (scenario.loanRepayment()) {
                loanRepayments++;
            }
            if (scenario.terminated()) {
                terminated++;
            }
        }
    }
}
