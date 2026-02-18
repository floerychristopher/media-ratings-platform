package mrp.server;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers;
    private String body;

    public HttpResponse(int statusCode) {
        this.statusCode = statusCode;
        this.statusMessage = getDefaultMessage(statusCode);
        this.headers = new HashMap<>();
        this.body = "";
        // Default to JSON — this is a REST API after all
        this.headers.put("Content-Type", "application/json");
    }

    // --- Fluent builder methods (makes code readable) ---

    public HttpResponse body(String body) {
        this.body = body;
        return this;
    }

    public HttpResponse header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * Converts this response to the raw HTTP string that goes over the wire.
     *
     * Example output:
     *   HTTP/1.1 200 OK
     *   Content-Type: application/json
     *   Content-Length: 27
     *
     *   {"message":"Hello World"}
     */
    public String toRawString() {
        StringBuilder sb = new StringBuilder();

        // Status line
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");

        // Content-Length must match the body bytes (not chars!) for proper HTTP
        byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        headers.put("Content-Length", String.valueOf(bodyBytes.length));

        // Headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        // Empty line separates headers from body
        sb.append("\r\n");

        // Body
        sb.append(body);

        return sb.toString();
    }

    // --- Factory methods for common responses ---

    public static HttpResponse ok(String jsonBody) {
        return new HttpResponse(200).body(jsonBody);
    }

    public static HttpResponse created(String jsonBody) {
        return new HttpResponse(201).body(jsonBody);
    }

    public static HttpResponse noContent() {
        return new HttpResponse(204);
    }

    public static HttpResponse badRequest(String message) {
        return new HttpResponse(400).body("{\"error\":\"" + message + "\"}");
    }

    public static HttpResponse unauthorized() {
        return new HttpResponse(401).body("{\"error\":\"Unauthorized\"}");
    }

    public static HttpResponse forbidden() {
        return new HttpResponse(403).body("{\"error\":\"Forbidden\"}");
    }

    public static HttpResponse notFound() {
        return new HttpResponse(404).body("{\"error\":\"Not Found\"}");
    }

    public static HttpResponse conflict(String message) {
        return new HttpResponse(409).body("{\"error\":\"" + message + "\"}");
    }

    public static HttpResponse internalError(String message) {
        return new HttpResponse(500).body("{\"error\":\"" + message + "\"}");
    }

    private String getDefaultMessage(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }
}