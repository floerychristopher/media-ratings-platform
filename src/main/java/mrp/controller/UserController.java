package mrp.controller;

import mrp.auth.TokenManager;
import mrp.model.User;
import mrp.server.HttpRequest;
import mrp.server.HttpResponse;
import mrp.service.UserService;
import mrp.util.JsonUtil;

import java.util.Map;

public class UserController {
    private final UserService userService;
    private final TokenManager tokenManager;

    public UserController(UserService userService, TokenManager tokenManager) {
        this.userService = userService;
        this.tokenManager = tokenManager;
    }

    /**
     * POST /api/users/register
     * Body: {"Username":"max", "Password":"secret"}
     */
    public HttpResponse register(HttpRequest request) {
        try {
            // Parse JSON body into a map (flexible — we don't need a DTO class)
            Map body = JsonUtil.fromJson(request.getBody(), Map.class);
            String username = (String) body.get("Username");
            String password = (String) body.get("Password");

            User user = userService.register(username, password);

            // Don't send the password back!
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
     * Body: {"Username":"max", "Password":"secret"}
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
     * Requires auth.
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
     * Requires auth. Only own profile.
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

    /**
     * Helper: Extracts the token from the request and looks up the user.
     * Returns null if not authenticated.
     */
    private User authenticate(HttpRequest request) {
        String token = request.getToken();
        return tokenManager.getUserByToken(token);
    }
}