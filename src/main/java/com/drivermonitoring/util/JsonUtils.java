// File: src/main/java/com/drivermonitoring/util/JsonUtils.java
// What is this file?
// Utility class for working with JSON metadata in events.
// Why is this needed?
// It provides helper methods for extracting and analyzing metadata stored as JSON strings.

package com.drivermonitoring.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // For handling Java 8 date/time types
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    // Create a single, reusable ObjectMapper instance
    private static final ObjectMapper objectMapper = createObjectMapper();

    // Private constructor to prevent instantiation
    private JsonUtils() {}

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register module for Java 8 Date/Time API (LocalDate, LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        // Configure as needed, e.g., disable writing dates as timestamps
        // mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Safely parses a JSON string into a Map<String, Object>.
     * Handles null, empty, or invalid JSON gracefully.
     * @param json JSON string to parse.
     * @return Map representation of the JSON, or an empty map if parsing fails or input is invalid.
     */
    public static Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("{}")) {
            return Collections.emptyMap();
        }

        try {
            // Use TypeReference to correctly deserialize into a Map<String, Object>
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON string: {}. Error: {}", json, e.getMessage());
            return Collections.emptyMap(); // Return empty map on error
        }
    }

    /**
     * Extracts a specific value from a JSON string by key.
     * @param json JSON string.
     * @param key The key of the value to extract.
     * @return Optional containing the value (as Object) if the key exists, otherwise Optional.empty().
     */
    public static Optional<Object> getValueFromJson(String json, String key) {
        Map<String, Object> map = parseJson(json);
        // Check if the key exists in the map
        return Optional.ofNullable(map.get(key));
    }

    /**
     * Safely extracts a value of a specific type from a JSON string by key.
     * Performs type checking and casting.
     * @param json JSON string.
     * @param key The key of the value to extract.
     * @param type The expected Class of the value.
     * @param <T> The expected type.
     * @return Optional containing the typed value if found and type matches, otherwise Optional.empty().
     */
    public static <T> Optional<T> getTypedValueFromJson(String json, String key, Class<T> type) {
        Optional<Object> valueOpt = getValueFromJson(json, key);

        if (valueOpt.isPresent()) {
            Object obj = valueOpt.get();
            // Check if the retrieved object is an instance of the expected type
            if (type.isInstance(obj)) {
                return Optional.of(type.cast(obj));
            } else {
                // Log a warning if the type does not match
                logger.warn("Value for key '{}' found, but is of type {} instead of expected type {}",
                            key, obj.getClass().getSimpleName(), type.getSimpleName());
                // Attempt conversion for common numeric types if applicable
                if (obj instanceof Number && Number.class.isAssignableFrom(type)) {
                    try {
                        if (type == Integer.class) return Optional.of(type.cast(((Number) obj).intValue()));
                        if (type == Long.class) return Optional.of(type.cast(((Number) obj).longValue()));
                        if (type == Double.class) return Optional.of(type.cast(((Number) obj).doubleValue()));
                        if (type == Float.class) return Optional.of(type.cast(((Number) obj).floatValue()));
                    } catch (ClassCastException | NumberFormatException e) {
                         logger.warn("Could not convert numeric value for key '{}' to type {}: {}", key, type.getSimpleName(), e.getMessage());
                    }
                }
            }
        }

        return Optional.empty(); // Return empty if key not found or type mismatch
    }

    /**
     * Safely converts a Java object into its JSON string representation.
     * Handles null objects gracefully.
     * @param object The object to serialize.
     * @return JSON string representation, or "{}" (empty JSON object) if the object is null or serialization fails.
     */
    public static String toJson(Object object) {
        if (object == null) {
            return "{}"; // Return empty JSON object for null input
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert object to JSON: {}. Error: {}", object, e.getMessage());
            return "{}"; // Return empty JSON object on error
        }
    }

    /**
     * Merges multiple JSON strings into a single JSON object string.
     * Later keys will overwrite earlier keys if they conflict.
     * @param jsonStrings Varargs array of JSON strings to merge.
     * @return A single JSON string representing the merged object, or "{}" if input is empty or all inputs are invalid.
     */
    public static String mergeJsonStrings(String... jsonStrings) {
        Map<String, Object> resultMap = new HashMap<>();

        if (jsonStrings != null) {
            for (String json : jsonStrings) {
                // Parse each non-empty JSON string and add its contents to the result map
                if (json != null && !json.trim().isEmpty()) {
                    Map<String, Object> map = parseJson(json);
                    resultMap.putAll(map); // putAll handles merging, overwriting duplicates
                }
            }
        }

        // Convert the final merged map back to a JSON string
        return toJson(resultMap);
    }

    /**
     * Checks if a JSON string contains a specific top-level key.
     * @param json JSON string.
     * @param key The key to check for.
     * @return true if the key exists at the top level, false otherwise.
     */
    public static boolean containsKey(String json, String key) {
        // Parse the JSON and check if the key is present in the resulting map
        return parseJson(json).containsKey(key);
    }

    /**
     * Extracts and potentially standardizes metadata fields, particularly from MediaPipe events.
     * This example standardizes 'earValue' to 'eyeAspectRatio' and adds a 'source'.
     * Adapt this method based on actual standardization needs.
     * @param json JSON string potentially containing MediaPipe metadata.
     * @return A Map with standardized keys and values.
     */
    public static Map<String, Object> extractAndStandardizeMetadata(String json) {
        Map<String, Object> originalMap = parseJson(json);
        Map<String, Object> standardizedMap = new HashMap<>();

        // Copy all original entries first
        standardizedMap.putAll(originalMap);

        // Example Standardization: Rename 'earValue' if present
        if (originalMap.containsKey("earValue")) {
            standardizedMap.put("eyeAspectRatio", originalMap.get("earValue"));
            // Optionally remove the old key if desired, but might be useful to keep both
            // standardizedMap.remove("earValue");
        }
        if (originalMap.containsKey("closeDuration")) {
             standardizedMap.put("eyeClosureDurationMs", originalMap.get("closeDuration"));
        }

        // Ensure 'source' is present, defaulting to 'unknown' if not found
        standardizedMap.putIfAbsent("source", "unknown");
        // If the original map had a source, it will be kept due to putAll above.
        // If you want to *force* a source (e.g., 'MediaPipe' if this method is specific), use put:
        // standardizedMap.put("source", "MediaPipe");

        // Add/modify other fields as needed based on analysis requirements

        return standardizedMap;
    }
}
