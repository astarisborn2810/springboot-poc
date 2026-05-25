package com.pearl.common.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    public static final Clock UTC_CLOCK = Clock.systemUTC();
    public static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;
    public static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateTimeUtil() {
    }

    public static Instant nowUtc() {
        return Instant.now(UTC_CLOCK);
    }

    public static OffsetDateTime nowOffsetUtc() {
        return OffsetDateTime.now(UTC_CLOCK);
    }

    public static String formatInstant(Instant instant) {
        return ISO_INSTANT.format(instant == null ? nowUtc() : instant);
    }

    public static Instant parseInstant(String value) {
        return Instant.parse(value);
    }

    public static String formatDate(LocalDate date) {
        return ISO_LOCAL_DATE.format(date);
    }

    public static LocalDate parseDate(String value) {
        return LocalDate.parse(value, ISO_LOCAL_DATE);
    }

    public static long millisUntil(Instant instant) {
        return Duration.between(nowUtc(), instant).toMillis();
    }

    public static OffsetDateTime toUtcOffset(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
