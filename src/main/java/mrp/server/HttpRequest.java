package mrp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;          // GET, POST, PUT, DELETE
    private String path;            // /api/users/login
    private String queryString;     // everything after '?'
    private Map<String, String> headers;
    private String body;
    private Map<String, String> queryParams;
    private Map<String, String> pathParams;  // filled in by router later

    public HttpRequest() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.pathParams = new HashMap<>();
    }

    /**
     * Reads raw bytes from the socket and parses them into this object.
     *
     * HTTP is line-based:
     *   Line 1:  METHOD /path HTTP/1.1
     *   Lines 2+: Header-Name: Header-Value
     *   Empty line
     *   Body (if Content-Length > 0)
     */
    public static HttpRequest parse(InputStream inputStream) throws IOException {
        HttpRequest request = new HttpRequest();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        // --- 1. Parse the request line ---
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Empty request");
        }

        // "POST /api/users/login HTTP/1.1" → ["POST", "/api/users/login", "HTTP/1.1"]
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            throw new IOException("Malformed request line: " + requestLine);
        }

        request.method = parts[0].toUpperCase();

        // Split path from query string: "/api/media?genre=action" → path + query
        String fullPath = parts[1];
        if (fullPath.contains("?")) {
            int idx = fullPath.indexOf('?');
            request.path = fullPath.substring(0, idx);
            request.queryString = fullPath.substring(idx + 1);
            request.parseQueryParams();
        } else {
            request.path = fullPath;
        }

        // --- 2. Parse headers ---
        // Headers come one per line until we hit an empty line
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colonIdx = headerLine.indexOf(':');
            if (colonIdx > 0) {
                String key = headerLine.substring(0, colonIdx).trim();
                String value = headerLine.substring(colonIdx + 1).trim();
                // Store headers lowercase for easy lookup
                request.headers.put(key.toLowerCase(), value);
            }
        }

        // --- 3. Parse body ---
        // Only read body if Content-Length header is present
        String contentLengthStr = request.headers.get("content-length");
        if (contentLengthStr != null) {
            int contentLength = Integer.parseInt(contentLengthStr.trim());
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int totalRead = 0;
                // We must read exactly contentLength chars — may take multiple reads
                while (totalRead < contentLength) {
                    int read = reader.read(bodyChars, totalRead, contentLength - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
                request.body = new String(bodyChars, 0, totalRead);
            }
        }

        return request;
    }

    /**
     * Parses "genre=action&year=2020&sort=title" into a map
     */
    private void parseQueryParams() {
        if (queryString == null || queryString.isEmpty()) return;

        for (String param : queryString.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                queryParams.put(kv[0], kv[1]);
            } else {
                queryParams.put(kv[0], "");
            }
        }
    }

    // --- Convenience methods ---

    public String getToken() {
        // "Bearer mustermann-mrpToken" → "mustermann-mrpToken"
        String auth = headers.get("authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

    // Getters
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public String getQueryParam(String key) { return queryParams.get(key); }
    public Map<String, String> getPathParams() { return pathParams; }
    public String getPathParam(String key) { return pathParams.get(key); }
    public void setPathParams(Map<String, String> pathParams) { this.pathParams = pathParams; }
}