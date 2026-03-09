package mrp.controller;

import mrp.auth.TokenManager;
import mrp.model.Rating;
import mrp.model.User;
import mrp.server.HttpRequest;
import mrp.server.HttpResponse;
import mrp.service.RatingService;
import mrp.util.JsonUtil;

import java.util.Map;

public class RatingController {
    private final RatingService ratingService;
    private final TokenManager tokenManager;

    public RatingController(RatingService ratingService, TokenManager tokenManager) {
        this.ratingService = ratingService;
        this.tokenManager = tokenManager;
    }

    // POST /api/media/{mediaId}/rate
    public HttpResponse rate(HttpRequest req) {
        try {
            User user = authenticate(req);
            if (user == null) return HttpResponse.unauthorized();

            int mediaId = Integer.parseInt(req.getPathParam("mediaId"));
            Map body = JsonUtil.fromJson(req.getBody(), Map.class);

            // JSON-Werte sicher extrahieren (können Integer oder String sein, je nach Parser)
            int stars = ((Number) body.get("stars")).intValue();
            String comment = (String) body.get("comment");

            Rating rating = ratingService.rateMedia(mediaId, user.getId(), stars, comment);
            return HttpResponse.created(JsonUtil.toJson(rating));

        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return HttpResponse.conflict(e.getMessage());
        } catch (Exception e) {
            return HttpResponse.internalError("Error creating rating");
        }
    }

    // PUT /api/ratings/{id}
    public HttpResponse update(HttpRequest req) {
        try {
            User user = authenticate(req);
            if (user == null) return HttpResponse.unauthorized();

            int ratingId = Integer.parseInt(req.getPathParam("id"));
            Map body = JsonUtil.fromJson(req.getBody(), Map.class);

            int stars = ((Number) body.get("stars")).intValue();
            String comment = (String) body.get("comment");

            boolean ok = ratingService.updateRating(ratingId, user.getId(), stars, comment);
            if (!ok) return HttpResponse.forbidden(); // Nicht gefunden oder nicht der eigene Kommentar

            return HttpResponse.ok("{\"message\":\"Rating updated\"}");

        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
    }

    // POST /api/ratings/{id}/like
    public HttpResponse like(HttpRequest req) {
        User user = authenticate(req);
        if (user == null) return HttpResponse.unauthorized();

        try {
            int ratingId = Integer.parseInt(req.getPathParam("id"));
            boolean ok = ratingService.likeRating(ratingId, user.getId());

            if (!ok) {
                // Wenn ok == false ist, existiert das Rating nicht oder der User hat schon geliked
                return HttpResponse.badRequest("Rating not found or already liked");
            }

            return HttpResponse.ok("{\"message\":\"Rating liked\"}");
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid ID");
        }
    }

    // POST /api/ratings/{id}/confirm
    public HttpResponse confirm(HttpRequest req) {
        try {
            User user = authenticate(req);
            if (user == null) return HttpResponse.unauthorized();

            int ratingId = Integer.parseInt(req.getPathParam("id"));
            boolean ok = ratingService.confirmRating(ratingId, user.getId());

            if (!ok) return HttpResponse.internalError("Could not confirm");
            return HttpResponse.ok("{\"message\":\"Comment confirmed\"}");

        } catch (SecurityException e) {
            return HttpResponse.forbidden();
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
    }

    private User authenticate(HttpRequest request) {
        String token = request.getToken();
        return tokenManager.getUserByToken(token);
    }
}