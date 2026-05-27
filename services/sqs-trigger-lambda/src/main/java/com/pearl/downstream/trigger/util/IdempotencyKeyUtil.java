package com.pearl.downstream.trigger.util;

import com.pearl.downstream.trigger.exception.TriggerProcessingException;
import com.pearl.downstream.trigger.model.S3EventMessage;
import com.pearl.downstream.trigger.model.StepFunctionInput;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

public final class IdempotencyKeyUtil {

    private static final String KEY_PREFIX = "s3event#";

    private IdempotencyKeyUtil() {
    }

    public static String forEvent(S3EventMessage message) {
        String rawKey = String.join("|",
                "v1",
                normalize(message.bucketName()),
                normalize(message.objectKey()),
                normalize(message.eventType()),
                eventIdentity(message));
        return KEY_PREFIX + sha256Hex(rawKey);
    }

    public static String forInput(StepFunctionInput input) {
        if (input.metadata() != null) {
            String metadataKey = input.metadata().get("idempotencyKey");
            if (metadataKey != null && !metadataKey.isBlank()) {
                return metadataKey;
            }
        }
        String rawKey = String.join("|",
                "v1",
                normalize(input.bucket()),
                normalize(input.key()),
                normalize(input.eventType()),
                eventIdentity(
                        input.sequencer(),
                        input.eTag(),
                        input.objectSize(),
                        input.eventTime(),
                        input.sourceMessageId()));
        return KEY_PREFIX + sha256Hex(rawKey);
    }

    public static String shortHash(String idempotencyKey, int length) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return sha256Hex("unknown").substring(0, length);
        }
        String normalized = idempotencyKey.startsWith(KEY_PREFIX)
                ? idempotencyKey.substring(KEY_PREFIX.length())
                : sha256Hex(idempotencyKey);
        return normalized.substring(0, Math.min(length, normalized.length()));
    }

    private static String eventIdentity(S3EventMessage message) {
        return eventIdentity(
                message.sequencer(),
                message.eTag(),
                message.objectSize(),
                message.eventTime(),
                message.sourceMessageId());
    }

    private static String eventIdentity(
            String sequencer,
            String eTag,
            Long objectSize,
            Instant eventTime,
            String fallback) {
        String combined = String.join(":",
                valueOrUnknown(sequencer),
                valueOrUnknown(eTag),
                valueOrUnknown(number(objectSize)),
                valueOrUnknown(instant(eventTime)));
        if (combined.replace("unknown", "").replace(":", "").isBlank()) {
            return firstNonBlank(fallback, "unknown");
        }
        return combined;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown";
    }

    private static String instant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static String number(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new TriggerProcessingException("Unable to create S3 idempotency key", ex);
        }
    }
}
