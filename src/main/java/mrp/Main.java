package mrp;

import mrp.auth.TokenManager;
import mrp.controller.UserController;
import mrp.db.DatabaseManager;
import mrp.repository.UserRepository;
import mrp.server.HttpServer;
import mrp.server.Router;
import mrp.service.UserService;

public class Main {
    public static void main(String[] args) {
        // --- Initialize infrastructure ---
        DatabaseManager db = DatabaseManager.getInstance();
        db.initializeSchema();

        TokenManager tokenManager = new TokenManager();

        // --- Build the dependency chain ---
        // Repository depends on DB
        UserRepository userRepository = new UserRepository(db);

        // Service depends on Repository + Auth
        UserService userService = new UserService(userRepository, tokenManager);

        // Controller depends on Service + Auth
        UserController userController = new UserController(userService, tokenManager);

        // --- Set up routing ---
        Router router = new Router();

        // Public endpoints (no auth needed)
        router.addRoute("POST", "/api/users/register", userController::register);
        router.addRoute("POST", "/api/users/login", userController::login);

        // Protected endpoints
        router.addRoute("GET", "/api/users/{username}/profile", userController::getProfile);
        router.addRoute("PUT", "/api/users/{username}/profile", userController::updateProfile);

        // MediaController, RatingController etc. will be added the same way

        // --- Start server ---
        HttpServer server = new HttpServer(9090, router);
        server.start();
    }
}