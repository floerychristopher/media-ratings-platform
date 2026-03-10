package mrp;

import mrp.auth.TokenManager;
import mrp.controller.RatingController;
import mrp.controller.UserController;
import mrp.controller.MediaController;
import mrp.controller.LeaderboardController;
import mrp.db.DatabaseManager;
import mrp.repository.RatingRepository;
import mrp.repository.UserRepository;
import mrp.repository.MediaRepository;
import mrp.server.HttpServer;
import mrp.server.Router;
import mrp.service.RatingService;
import mrp.service.UserService;
import mrp.service.MediaService;

public class Main {
    public static void main(String[] args) {
        // --- Initialize infrastructure ---
        DatabaseManager db = DatabaseManager.getInstance();
        db.initializeSchema();

        TokenManager tokenManager = new TokenManager();

        // --- Build the dependency chain ---

        // 1. Zuerst Media instanziieren
        MediaRepository mediaRepository = new MediaRepository(db);
        MediaService mediaService = new MediaService(mediaRepository);
        MediaController mediaController = new MediaController(mediaService, tokenManager);

        // 2. Dann Rating instanziieren (MUSS vor dem UserController passieren!)
        RatingRepository ratingRepository = new RatingRepository(db);
        RatingService ratingService = new RatingService(ratingRepository, mediaRepository);
        RatingController ratingController = new RatingController(ratingService, tokenManager);

        // 3. Dann User (hier den fertig gebauten mediaService und ratingService mitgeben!)
        UserRepository userRepository = new UserRepository(db);
        UserService userService = new UserService(userRepository, tokenManager);
        UserController userController = new UserController(userService, tokenManager, mediaService, ratingService);

        // 4. Zuletzt das Leaderboard
        LeaderboardController leaderboardController = new LeaderboardController(userService);

        // --- Set up routing ---
        Router router = new Router();

        // Public endpoints (no auth needed)
        router.addRoute("POST", "/api/users/register", userController::register);
        router.addRoute("POST", "/api/users/login", userController::login);

        // Protected user endpoints
        router.addRoute("GET", "/api/users/{username}/profile", userController::getProfile);
        router.addRoute("PUT", "/api/users/{username}/profile", userController::updateProfile);

        // Media endpoints
        router.addRoute("POST", "/api/media", mediaController::create);
        router.addRoute("GET", "/api/media/{id}", mediaController::getById);
        router.addRoute("GET", "/api/media", mediaController::getAll);
        router.addRoute("PUT", "/api/media/{id}", mediaController::update);
        router.addRoute("DELETE", "/api/media/{id}", mediaController::delete);

        // Rating Endpoints
        router.addRoute("POST", "/api/media/{mediaId}/rate", ratingController::rate);
        router.addRoute("PUT", "/api/ratings/{id}", ratingController::update);
        router.addRoute("POST", "/api/ratings/{id}/like", ratingController::like);
        router.addRoute("POST", "/api/ratings/{id}/confirm", ratingController::confirm);

        // Favoriten-Routen
        router.addRoute("POST", "/api/media/{id}/favorite", mediaController::addFavorite);
        router.addRoute("DELETE", "/api/media/{id}/favorite", mediaController::removeFavorite);
        router.addRoute("GET", "/api/users/{username}/favorites", userController::getFavorites);

        // Leaderboard & Rating History
        router.addRoute("GET", "/api/leaderboard", leaderboardController::getLeaderboard);
        router.addRoute("GET", "/api/users/{username}/ratings", userController::getRatingHistory);

        // --- Start server ---
        HttpServer server = new HttpServer(9090, router); // Port auf 9090 wie bei dir eingestellt
        server.start();
    }
}