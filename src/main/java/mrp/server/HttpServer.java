package mrp.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private final int port;
    private final Router router;
    private final ExecutorService threadPool;
    private boolean running;

    public HttpServer(int port, Router router) {
        this.port = port;
        this.router = router;
        // Each request gets its own thread from the pool (initialize thread pool)
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    public void start() {
        running = true;
        System.out.println("Server starting on port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on http://localhost:" + port);

            while (running) {
                // This blocks until a client connects
                Socket clientSocket = serverSocket.accept();

                // Handle the request in a separate thread (don't block the accept loop)
                threadPool.submit(() -> handleConnection(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void handleConnection(Socket clientSocket) {
        try (clientSocket) { // auto-closes when done
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();

            // 1. Parse the raw HTTP into our request object
            HttpRequest request = HttpRequest.parse(input);
            System.out.println(request.getMethod() + " " + request.getPath());

            // 2. Route it to the correct handler
            HttpResponse response;
            try {
                response = router.route(request);
            } catch (Exception e) {
                // Catch any unhandled exception → 500
                System.err.println("Error handling request: " + e.getMessage());
                e.printStackTrace();
                response = HttpResponse.internalError("Internal server error");
            }

            // 3. Send the response back over the wire
            String rawResponse = response.toRawString();
            output.write(rawResponse.getBytes(StandardCharsets.UTF_8));
            output.flush();

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        threadPool.shutdown();
    }
}