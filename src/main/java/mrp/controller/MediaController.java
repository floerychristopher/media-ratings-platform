package mrp.controller;

import mrp.model.Media;
import mrp.model.User;
import mrp.service.MediaService;
import mrp.auth.TokenManager;
import mrp.server.HttpRequest;
import mrp.server.HttpResponse;
import mrp.util.JsonUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class MediaController {
    private final MediaService service;
    private final TokenManager tokenManager;

    public MediaController(MediaService service, TokenManager tokenManager) {
        this.service = service;
        this.tokenManager = tokenManager;
    }

    public HttpResponse create(HttpRequest req) {
        try {
            User user = authenticate(req);
            if (user == null) return HttpResponse.unauthorized();

            Media media = JsonUtil.fromJson(req.getBody(), Media.class);
            media.setCreatedBy(user.getId());
            Media created = service.createMedia(media);

            return HttpResponse.created(JsonUtil.toJson(created));
        } catch (SQLException e) {
            return HttpResponse.internalError(e.getMessage());
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid JSON");
        }
    }

    public HttpResponse getById(HttpRequest req) {
        try {
            int id = Integer.parseInt(req.getPathParam("id"));
            Media media = service.getMediaById(id);
            if (media == null) return HttpResponse.notFound();

            return HttpResponse.ok(JsonUtil.toJson(media));
        } catch (NumberFormatException e) {
            return HttpResponse.badRequest("ID must be a number");
        } catch (Exception e) {
            return HttpResponse.internalError(e.getMessage());
        }
    }

    public HttpResponse getAll(HttpRequest req) {
        try {
            // Query-Parameter aus URL holen
            Map<String, String> queryParams = req.getQueryParams();

            List<Media> results = service.searchAndFilter(queryParams);

            return HttpResponse.ok(JsonUtil.toJson(results));

        } catch (NumberFormatException e) {
            return HttpResponse.badRequest("Invalid number format in query parameters");
        } catch (SQLException e) {
            return HttpResponse.internalError("Database error during search: " + e.getMessage());
        }
    }

    public HttpResponse update(HttpRequest req) {
        try {
            User user = authenticate(req);
            if (user == null) return HttpResponse.unauthorized();

            int id = Integer.parseInt(req.getPathParam("id"));

            //vorhandenes Media-Objekt laden, um z.B createdAt zu erhalten
            Media existingMedia = service.getMediaById(id);
            if (existingMedia == null) return HttpResponse.notFound();

            Media media = JsonUtil.fromJson(req.getBody(), Media.class);
            media.setId(id);
            media.setCreatedAt(existingMedia.getCreatedAt()); // Datum aus DB

            boolean ok = service.updateMedia(media, user.getId());
            if (!ok) return HttpResponse.forbidden(); // Gibt an, dass man nicht der Creator ist

            return HttpResponse.ok(JsonUtil.toJson(media));
        } catch (SQLException e) {
            return HttpResponse.internalError(e.getMessage());
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid JSON or ID");
        }
    }

    public HttpResponse delete(HttpRequest req) {
        try {
            User user = authenticate(req);
            if (user == null) return HttpResponse.unauthorized();

            int id = Integer.parseInt(req.getPathParam("id"));
            boolean ok = service.deleteMedia(id, user.getId());
            if (!ok) return HttpResponse.forbidden();

            return HttpResponse.noContent();
        } catch (SQLException e) {
            return HttpResponse.internalError(e.getMessage());
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid ID");
        }
    }

     //Extrahieren des Users analog zum UserController
    private User authenticate(HttpRequest request) {
        String token = request.getToken();
        return tokenManager.getUserByToken(token);
    }

    // --- Favoriten ---

    public HttpResponse addFavorite(HttpRequest req) {
        User user = authenticate(req);
        if (user == null) return HttpResponse.unauthorized();

        try {
            int mediaId = Integer.parseInt(req.getPathParam("id"));
            service.addFavorite(mediaId, user.getId());
            return HttpResponse.ok("{\"message\":\"Added to favorites\"}");
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid ID");
        }
    }

    public HttpResponse removeFavorite(HttpRequest req) {
        User user = authenticate(req);
        if (user == null) return HttpResponse.unauthorized();

        try {
            int mediaId = Integer.parseInt(req.getPathParam("id"));
            service.removeFavorite(mediaId, user.getId());
            return HttpResponse.ok("{\"message\":\"Removed from favorites\"}");
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid ID");
        }
    }
}