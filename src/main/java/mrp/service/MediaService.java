package mrp.service;

import mrp.model.Media;
import mrp.repository.MediaRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class MediaService {
    private final MediaRepository repo;

    public MediaService(MediaRepository repo) {
        this.repo = repo;
    }

    public Media createMedia(Media media) throws SQLException {
        return repo.create(media);
    }

    public Media getMediaById(int id) throws SQLException {
        return repo.getById(id);
    }

    public List<Media> getAllMedia() throws SQLException {
        return repo.getAll();
    }

    public boolean updateMedia(Media media, int requestingUserId) throws SQLException {
        media.setCreatedBy(requestingUserId); // ensure user only edits own media
        return repo.update(media);
    }

    public boolean deleteMedia(int mediaId, int requestingUserId) throws SQLException {
        return repo.delete(mediaId, requestingUserId);
    }

    // --- Favoriten ---

    public boolean addFavorite(int mediaId, int userId) {
        return repo.addFavorite(mediaId, userId);
    }

    public boolean removeFavorite(int mediaId, int userId) {
        return repo.removeFavorite(mediaId, userId);
    }

    public List<Media> getFavoritesByUserId(int userId) throws SQLException {
        return repo.getFavoritesByUserId(userId);
    }

    public List<Media> searchAndFilter(Map<String, String> queryParams) throws SQLException {
        // Falls gar keine Parameter da sind, könnten wir auch direkt repo.getAll() aufrufen,
        // aber unsere neue Suchfunktion deckt das als Standardfall ab!
        return repo.searchAndFilter(queryParams);
    }
}