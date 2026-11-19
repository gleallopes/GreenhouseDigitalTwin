package com.greenhousedigitaltwin.telemetry.application;

import java.util.Locale;

final class TelemetryEnumParser {

    private TelemetryEnumParser() {
    }

    static <E extends Enum<E>> E parse(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
