package mrp.service;

import mrp.model.Media;
import mrp.repository.MediaRepository;

import java.sql.SQLException;
import java.util.List;

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
}