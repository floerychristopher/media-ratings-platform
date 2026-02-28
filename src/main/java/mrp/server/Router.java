package mrp.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Router {

    /**
     * A route is: METHOD + pattern + handler function
     * Pattern example: "/api/media/{id}"
     * The handler takes an HttpRequest and returns an HttpResponse.
     */
    private record Route(
            String method,
            String pattern,           // e.g. "/api/media/{id}"
            String[] patternParts,    // ["api", "media", "{id}"]
            Function<HttpRequest, HttpResponse> handler
    ) {}

    private final List<Route> routes = new ArrayList<>();

    /**
     * Register a route. Usage:
     *   router.addRoute("GET", "/api/media/{id}", mediaController::getById);
     *   router.addRoute("POST", "/api/users/login", userController::login);
     */
    public void addRoute(String method, String pattern, Function<HttpRequest, HttpResponse> handler) {
        // Split pattern into parts for matching
        // "/api/media/{id}" → ["api", "media", "{id}"]
        String[] parts = pattern.substring(1).split("/"); // skip leading '/'
        routes.add(new Route(method.toUpperCase(), pattern, parts, handler));
    }

    /**
     * Finds the matching route for a request.
     *
     * How matching works:
     *   Request path: /api/media/42
     *   Pattern:      /api/media/{id}
     *
     *   "api" == "api"       ✓ literal match
     *   "media" == "media"   ✓ literal match
     *   "42" vs "{id}"       ✓ wildcard match → pathParams["id"] = "42"
     */
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
                    // Literal part doesn't match
                    matches = false;
                    break;
                }
            }

            if (matches) {
                // Inject extracted path params into the request
                request.setPathParams(pathParams);
                // Handler (Lambda Funktion) der Route die gematcht hat wird aufgerufen und request auf diese Funktion applied damit?
                return route.handler().apply(request);
            }
        }

        return HttpResponse.notFound();
    }
}