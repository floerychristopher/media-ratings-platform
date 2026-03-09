package mrp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mrp.model.Media;
import mrp.service.MediaService;
import mrp.auth.TokenManager;
import mrp.server.HttpRequest;
import mrp.server.HttpResponse;

import java.sql.SQLException;
import java.util.List;

public class MediaController {
    private final MediaService service;
    private final TokenManager tokenManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public MediaController(MediaService service, TokenManager tokenManager) {
        this.service = service;
        this.tokenManager = tokenManager;
    }

    public HttpResponse create(HttpRequest req) {
        try {
            String token = req.getToken();
            User user = tokenManager.getUserByToken(token);
            if (user == null) return HttpResponse.unauthorized();

            Media media = mapper.readValue(req.getBody(), Media.class);
            media.setCreatedBy(user.getId()); // <-- use user.getId() from TokenManager
            Media created = service.createMedia(media);

            return HttpResponse.created(mapper.writeValueAsString(created));
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
            return HttpResponse.ok(mapper.writeValueAsString(media));
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid ID");
        }
    }

    public HttpResponse getAll(HttpRequest req) {
        try {
            List<Media> all = service.getAllMedia();
            return HttpResponse.ok(mapper.writeValueAsString(all));
        } catch (SQLException e) {
            return HttpResponse.internalError(e.getMessage());
        }
    }

    public HttpResponse update(HttpRequest req) {
        try {
            String token = req.getToken();
            User user = tokenManager.getUserByToken(token);
            if (user == null) return HttpResponse.unauthorized();

            int id = Integer.parseInt(req.getPathParam("id"));
            Media media = mapper.readValue(req.getBody(), Media.class);
            media.setId(id);

            boolean ok = service.updateMedia(media, user.getId());
            if (!ok) return HttpResponse.forbidden();
            return HttpResponse.ok(mapper.writeValueAsString(media));
        } catch (SQLException e) {
            return HttpResponse.internalError(e.getMessage());
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid JSON or ID");
        }
    }

    public HttpResponse delete(HttpRequest req) {
        try {
            String token = req.getToken();
            User user = tokenManager.getUserByToken(token);
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
}