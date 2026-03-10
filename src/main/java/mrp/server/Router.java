package mrp.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Router {

     // Route: METHOD + pattern + handler function
     // Pattern: "/api/media/{id}"
     // Handler takes HttpRequest and returns HttpResponse
    private record Route(
            String method,
            String pattern,           // e.g. "/api/media/{id}"
            String[] patternParts,    // ["api", "media", "{id}"]
            Function<HttpRequest, HttpResponse> handler
    ) {}

    private final List<Route> routes = new ArrayList<>();

    // Register a route
    public void addRoute(String method, String pattern, Function<HttpRequest, HttpResponse> handler) {
        // Split pattern into parts for matching
        // "/api/media/{id}" -> ["api", "media", "{id}"]
        String[] parts = pattern.substring(1).split("/"); // skip leading '/'
        routes.add(new Route(method.toUpperCase(), pattern, parts, handler));
    }

    // Finds matching route for a request
    public HttpResponse route(HttpRequest request) {
        String[] requestParts = request.getPath().substring(1).split("/");

        for (Route route : routes) {
            // Methode überprüfen
            if (!route.method().equals(request.getMethod())) continue;
            // Länge der Parts überprüfen
            if (route.patternParts().length != requestParts.length) continue;

            Map<String, String> pathParams = new HashMap<>();
            boolean matches = true;

            // Jeden pattern part der jeweiligen route durchgehen und mit dem jeweiligen part des requests vergleichen
            for (int i = 0; i < route.patternParts().length; i++) {
                String patternPart = route.patternParts()[i];
                String requestPart = requestParts[i];

                if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                    // Its a path variable -> extract it
                    // geschwungene Klammern entfernen
                    String paramName = patternPart.substring(1, patternPart.length() - 1);
                    pathParams.put(paramName, requestPart);
                } else if (!patternPart.equals(requestPart)) {
                    // Literal part doesnt match
                    matches = false;
                    break;
                }
            }

            if (matches) {
                // Inject extracted path params into the request
                request.setPathParams(pathParams);
                // Führe die gespeicherte Funktion (Handler) mit request als Übergabeparameter aus
                return route.handler().apply(request);
            }
        }

        return HttpResponse.notFound();
    }
}