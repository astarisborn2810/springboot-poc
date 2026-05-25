package com.pearl.downstream.trigger.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

public final class S3ObjectKeyUtil {

    private S3ObjectKeyUtil() {
    }

    public static String decode(String key) {
        if (key == null) {
            return null;
        }
        return URLDecoder.decode(key, StandardCharsets.UTF_8);
    }

    public static Optional<String> segment(String key, int index) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String[] parts = key.split("/");
        if (index < 0 || index >= parts.length || parts[index].isBlank()) {
            return Optional.empty();
        }
        return Optional.of(parts[index]);
    }

    public static Optional<String> normalizeDataType(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("financial") || normalized.equals("fin")) {
            return Optional.of("financial");
        }
        if (normalized.contains("indicative") || normalized.equals("ind")) {
            return Optional.of("indicative");
        }
        return Optional.of(normalized.replaceAll("[^a-z0-9_-]", "-"));
    }
}
