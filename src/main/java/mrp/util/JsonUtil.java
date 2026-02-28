package mrp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {
    // One ObjectMapper for the entire app — it's thread-safe
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())                    // handles LocalDateTime
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // "2024-01-15T10:30:00" not epoch
            .configure(com.fasterxml.jackson.databind.DeserializationFeature
                    .FAIL_ON_UNKNOWN_PROPERTIES, false);             // ignore extra JSON fields

    /**
     * Object → JSON string
     * Usage: JsonUtil.toJson(user)  →  {"username":"max","bio":"hello"}
     */
    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * JSON string → Object
     * Usage: JsonUtil.fromJson(body, User.class)  →  User object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON deserialization failed: " + e.getMessage(), e);
        }
    }
}