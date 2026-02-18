package mrp;

import mrp.server.HttpServer;
import mrp.server.HttpRequest;
import mrp.server.HttpResponse;
import mrp.server.Router;

public class Main {
    public static void main(String[] args) {
        Router router = new Router();

        // Test endpoint — prove the server works
        router.addRoute("GET", "/api/hello", (HttpRequest req) -> {
            return HttpResponse.ok("{\"message\":\"Hello, MRP!\"}");
        });
        // addRoute registriert genau diese Route. Kommt jetzt ein Request mit genau dieser Struktur, wird die Lambda-Funktion ausgeführt

        // Echo endpoint — returns whatever you POST
        router.addRoute("POST", "/api/echo", (HttpRequest req) -> {
            return HttpResponse.ok(req.getBody());
        });

        // Path param test — GET /api/test/42 → {"id":"42"}
        router.addRoute("GET", "/api/test/{id}", (HttpRequest req) -> {
            String id = req.getPathParam("id");
            return HttpResponse.ok("{\"id\":\"" + id + "\"}");
        });

        // Dem neu erstellten Server wird router übergeben damit der Server weiß wie er Request behandeln soll.
        HttpServer server = new HttpServer(9090, router);
        server.start();
    }
}