package mrp.controller;

import mrp.auth.TokenManager;
import mrp.model.Media;
import mrp.model.Rating;
import mrp.model.User;
import mrp.server.HttpRequest;
import mrp.server.HttpResponse;
import mrp.service.UserService;
import mrp.util.JsonUtil;

import java.util.List;
import java.util.Map;

public class UserController {
    private final UserService userService;
    private final TokenManager tokenManager;
    private final mrp.service.MediaService mediaService;
    private final mrp.service.RatingService ratingService;

    public UserController(UserService userService, TokenManager tokenManager, mrp.service.MediaService mediaService, mrp.service.RatingService ratingService) {
        this.userService = userService;
        this.tokenManager = tokenManager;
        this.mediaService = mediaService;
        this.ratingService = ratingService;
    }

    /**
     * POST /api/users/register
     * Body: {"Username":"max", "Password":"password123"}
     */
    public HttpResponse register(HttpRequest request) {
        try {
            // Parse JSON body into map
            Map body = JsonUtil.fromJson(request.getBody(), Map.class);
            String username = (String) body.get("Username");
            String password = (String) body.get("Password");

            User user = userService.register(username, password);

            // Dont send the pw back
            user.setPassword(null);
            return HttpResponse.created(JsonUtil.toJson(user));

        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return HttpResponse.conflict(e.getMessage());
        } catch (Exception e) {
            return HttpResponse.internalError(e.getMessage());
        }
    }

    /**
     * POST /api/users/login
     * Body: {"Username":"max", "Password":"passwort123"}
     * Returns: token string
     */
    public HttpResponse login(HttpRequest request) {
        try {
            Map body = JsonUtil.fromJson(request.getBody(), Map.class);
            String username = (String) body.get("Username");
            String password = (String) body.get("Password");

            String token = userService.login(username, password);

            return HttpResponse.ok("{\"token\":\"" + token + "\"}");

        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return HttpResponse.internalError(e.getMessage());
        }
    }

    /**
     * GET /api/users/{username}/profile
     * Requires authentication
     */
    public HttpResponse getProfile(HttpRequest request) {
        try {
            // Auth check
            User authUser = authenticate(request);
            if (authUser == null) return HttpResponse.unauthorized();

            String username = request.getPathParam("username");
            User user = userService.getProfile(username);

            // Build a profile response (without password, with stats later)
            user.setPassword(null);
            return HttpResponse.ok(JsonUtil.toJson(user));

        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound();
        } catch (Exception e) {
            return HttpResponse.internalError(e.getMessage());
        }
    }

    /**
     * PUT /api/users/{username}/profile
     * Body: {"Bio":"Hello, I love movies!"}
     * Requires authentication, only your own profile
     */
    public HttpResponse updateProfile(HttpRequest request) {
        try {
            User authUser = authenticate(request);
            if (authUser == null) return HttpResponse.unauthorized();

            String username = request.getPathParam("username");
            Map body = JsonUtil.fromJson(request.getBody(), Map.class);
            String bio = (String) body.get("Bio");

            User updated = userService.updateProfile(authUser, username, bio);
            updated.setPassword(null);
            return HttpResponse.ok(JsonUtil.toJson(updated));

        } catch (SecurityException e) {
            return HttpResponse.forbidden();
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            return HttpResponse.internalError(e.getMessage());
        }
    }

    // Helper: Extracts token from the request and looks up user (return null if not authenticated)
    private User authenticate(HttpRequest request) {
        String token = request.getToken();
        return tokenManager.getUserByToken(token);
    }

    /**
     * GET /api/users/{username}/favorites
     */
    public HttpResponse getFavorites(HttpRequest req) {
        try {
            User authUser = authenticate(req);
            if (authUser == null) return HttpResponse.unauthorized();

            String username = req.getPathParam("username");
            // Check ob user existiert
            User targetUser = userService.getProfile(username);

            // Favoriten über MediaService laden
            List<Media> favorites = mediaService.getFavoritesByUserId(targetUser.getId());

            return HttpResponse.ok(JsonUtil.toJson(favorites));

        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound();
        } catch (Exception e) {
            return HttpResponse.internalError(e.getMessage());
        }
    }

    /**
     * GET /api/users/{username}/ratings
     */
    public HttpResponse getRatingHistory(HttpRequest req) {
        try {
            User authUser = authenticate(req);
            if (authUser == null) return HttpResponse.unauthorized();

            String username = req.getPathParam("username");
            // Profil laden (prüft ob User existiert)
            User targetUser = userService.getProfile(username);

            java.util.List<Rating> history = ratingService.getRatingsByUserId(targetUser.getId());
            return HttpResponse.ok(JsonUtil.toJson(history));

        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound();
        } catch (Exception e) {
            return HttpResponse.internalError(e.getMessage());
        }
    }
}