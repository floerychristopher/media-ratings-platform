package mrp.service;

import mrp.model.Media;
import mrp.model.Rating;
import mrp.repository.MediaRepository;
import mrp.repository.RatingRepository;

import java.sql.SQLException;

public class RatingService {
    private final RatingRepository ratingRepo;
    private final MediaRepository mediaRepo;

    public RatingService(RatingRepository ratingRepo, MediaRepository mediaRepo) {
        this.ratingRepo = ratingRepo;
        this.mediaRepo = mediaRepo;
    }

    public Rating rateMedia(int mediaId, int userId, int stars, String comment) throws Exception {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Stars must be between 1 and 5");
        }

        // Prüfen, ob das Medium existiert
        Media media = mediaRepo.getById(mediaId);
        if (media == null) {
            throw new IllegalArgumentException("Media not found");
        }

        Rating rating = new Rating();
        rating.setMediaId(mediaId);
        rating.setUserId(userId);
        rating.setStars(stars);
        rating.setComment(comment);

        try {
            return ratingRepo.create(rating);
        } catch (SQLException e) {
            // PostgreSQL wirft Fehler, wenn der User dieses Medium schon bewertet hat (UNIQUE Constraint)
            throw new IllegalStateException("User has already rated this media");
        }
    }

    public boolean updateRating(int ratingId, int userId, int stars, String comment) throws Exception {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Stars must be between 1 and 5");
        }
        return ratingRepo.update(ratingId, userId, stars, comment);
    }

    public boolean likeRating(int ratingId, int userId) {
        return ratingRepo.addLike(ratingId, userId);
    }

    public boolean confirmRating(int ratingId, int requestingUserId) throws Exception {
        Rating rating = ratingRepo.getById(ratingId);
        if (rating == null) {
            throw new IllegalArgumentException("Rating not found");
        }

        Media media = mediaRepo.getById(rating.getMediaId());
        if (media == null || media.getCreatedBy() != requestingUserId) {
            throw new SecurityException("Only the creator of the media can confirm comments");
        }

        return ratingRepo.confirmComment(ratingId);
    }

    public java.util.List<Rating> getRatingsByUserId(int userId) throws SQLException {
        return ratingRepo.getByUserId(userId);
    }
}