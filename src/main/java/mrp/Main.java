package mrp;

import mrp.auth.TokenManager;
import mrp.controller.UserController;
import mrp.controller.MediaController; // <-- NEU
import mrp.db.DatabaseManager;
import mrp.repository.UserRepository;
import mrp.repository.MediaRepository; // <-- NEU
import mrp.server.HttpServer;
import mrp.server.Router;
import mrp.service.UserService;
import mrp.service.MediaService; // <-- NEU

public class Main {
    public static void main(String[] args) {
        // --- Initialize infrastructure ---
        DatabaseManager db = DatabaseManager.getInstance();
        db.initializeSchema();

        TokenManager tokenManager = new TokenManager();

        // --- Build the dependency chain for Users ---
        UserRepository userRepository = new UserRepository(db);
        UserService userService = new UserService(userRepository, tokenManager);
        UserController userController = new UserController(userService, tokenManager);

        // --- Build the dependency chain for Media --- (NEU)
        MediaRepository mediaRepository = new MediaRepository(db);
        MediaService mediaService = new MediaService(mediaRepository);
        MediaController mediaController = new MediaController(mediaService, tokenManager);

        // --- Set up routing ---
        Router router = new Router();

        // Public endpoints (no auth needed)
        router.addRoute("POST", "/api/users/register", userController::register);
        router.addRoute("POST", "/api/users/login", userController::login);

        // Protected user endpoints
        router.addRoute("GET", "/api/users/{username}/profile", userController::getProfile);
        router.addRoute("PUT", "/api/users/{username}/profile", userController::updateProfile);

        // Media endpoints (NEU)
        router.addRoute("POST", "/api/media", mediaController::create);
        router.addRoute("GET", "/api/media/{id}", mediaController::getById);
        router.addRoute("GET", "/api/media", mediaController::getAll);
        router.addRoute("PUT", "/api/media/{id}", mediaController::update);
        router.addRoute("DELETE", "/api/media/{id}", mediaController::delete);

        // --- Start server ---
        HttpServer server = new HttpServer(9090, router); // Port auf 8080 angepasst, wie in den Tests
        server.start();
    }
}