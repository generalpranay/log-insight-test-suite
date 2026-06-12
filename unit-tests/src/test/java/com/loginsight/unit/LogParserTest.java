package com.loginsight.unit;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the JSON log parser that turns raw ingest payloads into structured
 * {@link ParsedLog} values.
 */
class LogParserTest {

    private final LogParser parser = new LogParser();

    @Test
    @DisplayName("A well-formed JSON log is parsed into structured fields")
    void parsesValidJsonLog() {
        String payload = """
                {
                  "timestamp": "2026-06-12T10:15:30Z",
                  "level": "ERROR",
                  "service": "api",
                  "message": "Upstream timeout while calling billing"
                }
                """;

        Optional<ParsedLog> parsed = parser.parse(payload);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().level()).isEqualTo("ERROR");
        assertThat(parsed.get().service()).isEqualTo("api");
        assertThat(parsed.get().message()).isEqualTo("Upstream timeout while calling billing");
        assertThat(parsed.get().timestamp()).isEqualTo("2026-06-12T10:15:30Z");
    }

    @Test
    @DisplayName("Garbage input raises a LogParseException")
    void throwsOnMalformedLog() {
        assertThatThrownBy(() -> parser.parse("NOT_VALID_JSON{{{"))
                .isInstanceOf(LogParseException.class)
                .hasMessageContaining("malformed");
    }

    @Test
    @DisplayName("Blank input yields an empty result rather than an error")
    void handlesEmptyPayload() {
        assertThat(parser.parse("   ")).isEmpty();
        assertThat(parser.parse("")).isEmpty();
    }

    /**
     * Structured representation of a single log line.
     */
    record ParsedLog(String timestamp, String level, String service, String message) {
    }

    /**
     * Raised when a non-blank payload cannot be interpreted as a log object.
     */
    static class LogParseException extends RuntimeException {
        LogParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Parses raw JSON log payloads. Blank input is treated as "nothing to do"
     * and returns an empty {@link Optional}; structurally invalid input raises
     * {@link LogParseException}.
     */
    static class LogParser {

        Optional<ParsedLog> parse(String payload) {
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }

            try {
                JSONObject json = new JSONObject(payload);
                ParsedLog log = new ParsedLog(
                        json.optString("timestamp", null),
                        json.optString("level", null),
                        json.optString("service", null),
                        json.optString("message", null));
                return Optional.of(log);
            } catch (JSONException e) {
                throw new LogParseException("malformed log payload: " + payload, e);
            }
        }
    }
}
