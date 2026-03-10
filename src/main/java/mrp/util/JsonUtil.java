package mrp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {
    // one ObjectMapper for entire app
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())                    // handles LocalDateTime
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature
                    .FAIL_ON_UNKNOWN_PROPERTIES, false);             // ignore extra JSON fields

    /**
     * Object -> JSON string
     * Usage: JsonUtil.toJson(user)  ->  {"username":"max","bio":"hello"}
     */
    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * JSON string ->  Object
     * Usage: JsonUtil.fromJson(body, User.class)  ->  User object
     */
    public static <T> T fromJson(String json, Class<T> klasse) {
        try {
            return mapper.readValue(json, klasse);
        } catch (Exception e) {
            throw new RuntimeException("JSON deserialization failed: " + e.getMessage(), e);
        }
    }
}