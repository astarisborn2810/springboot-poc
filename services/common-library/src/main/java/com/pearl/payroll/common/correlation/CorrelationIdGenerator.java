package com.pearl.payroll.common.correlation;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

public final class CorrelationIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CorrelationIdGenerator() {
    }

    public static String newCorrelationId() {
        byte[] randomBytes = new byte[8];
        RANDOM.nextBytes(randomBytes);
        String timestamp = Long.toUnsignedString(Instant.now(Clock.systemUTC()).toEpochMilli(), 36);
        return "corr-" + timestamp + "-" + HexFormat.of().formatHex(randomBytes);
    }
}
