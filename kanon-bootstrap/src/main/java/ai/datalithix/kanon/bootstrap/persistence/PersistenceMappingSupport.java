package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.model.AuditMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class PersistenceMappingSupport {
    private PersistenceMappingSupport() {}

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    static String toJson(Object value) {
        if (value == null) return null;
        try { return JSON.writeValueAsString(value); }
        catch (Exception e) { throw new RuntimeException("JSON serialization error", e); }
    }

    static <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) return null;
        try { return JSON.readValue(json, type); }
        catch (Exception e) { throw new RuntimeException("JSON deserialization error", e); }
    }

    static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try { return JSON.readValue(json, typeRef); }
        catch (Exception e) { throw new RuntimeException("JSON deserialization error", e); }
    }

    static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    static Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    static Long millis(Duration duration) {
        return duration == null ? null : duration.toMillis();
    }

    static Duration duration(Long millis) {
        return millis == null ? null : Duration.ofMillis(millis);
    }

    static AuditMetadata audit(ResultSet resultSet) throws SQLException {
        return new AuditMetadata(
                instant(resultSet, "created_at"),
                resultSet.getString("created_by"),
                instant(resultSet, "updated_at"),
                resultSet.getString("updated_by"),
                resultSet.getLong("audit_version")
        );
    }

    static String joinStrings(Iterable<String> values) {
        if (values == null) {
            return null;
        }
        String joined = "";
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joined = joined.isEmpty() ? value : joined + "," + value;
            }
        }
        return joined.isEmpty() ? null : joined;
    }

    static <E extends Enum<E>> String joinEnums(Iterable<E> values) {
        if (values == null) {
            return null;
        }
        String joined = "";
        for (E value : values) {
            if (value != null) {
                joined = joined.isEmpty() ? value.name() : joined + "," + value.name();
            }
        }
        return joined.isEmpty() ? null : joined;
    }

    static List<String> stringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    static Set<String> stringSet(String value) {
        return Set.copyOf(stringList(value));
    }

    static <E extends Enum<E>> Set<E> enumSet(String value, Function<String, E> mapper) {
        return stringList(value).stream().map(mapper).collect(Collectors.toUnmodifiableSet());
    }

    static <E extends Enum<E>> E optionalEnum(String value, Function<String, E> mapper) {
        return value == null || value.isBlank() ? null : mapper.apply(value);
    }
}
