package com.pearl.common.util;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public final class IdGeneratorUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter BATCH_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT).withZone(java.time.ZoneOffset.UTC);

    private IdGeneratorUtil() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String compactUuid() {
        return uuid().replace("-", "");
    }

    public static String batchId() {
        return "batch-" + BATCH_TIMESTAMP.format(DateTimeUtil.nowUtc()) + "-" + randomHex(4);
    }

    public static String requestId() {
        return "req-" + compactUuid();
    }

    public static String executionId() {
        return "exec-" + compactUuid();
    }

    public static String randomHex(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
