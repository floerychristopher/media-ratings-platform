package mrp.repository;

import mrp.db.DatabaseManager;
import mrp.model.Rating;

import java.sql.*;

public class RatingRepository {
    private final DatabaseManager db;

    public RatingRepository(DatabaseManager db) {
        this.db = db;
    }

    // Erstellt ein neues Rating
    public Rating create(Rating rating) throws SQLException {
        String sql = "INSERT INTO ratings (media_id, user_id, stars, comment, comment_visible) VALUES (?, ?, ?, ?, false) RETURNING id, created_at";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rating.getMediaId());
            stmt.setInt(2, rating.getUserId());
            stmt.setInt(3, rating.getStars());
            stmt.setString(4, rating.getComment());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                rating.setId(rs.getInt("id"));
                rating.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                rating.setCommentVisible(false);
            }
        }
        return rating;
    }

    // Findet ein Rating anhand seiner ID
    public Rating getById(int id) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Rating r = new Rating();
                r.setId(rs.getInt("id"));
                r.setMediaId(rs.getInt("media_id"));
                r.setUserId(rs.getInt("user_id"));
                r.setStars(rs.getInt("stars"));
                r.setComment(rs.getString("comment"));
                r.setCommentVisible(rs.getBoolean("comment_visible"));
                r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return r;
            }
        }
        return null;
    }

    // Aktualisiert Sterne und Kommentar (nur durch den Autor des Ratings)
    public boolean update(int ratingId, int userId, int stars, String comment) throws SQLException {
        String sql = "UPDATE ratings SET stars = ?, comment = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, stars);
            stmt.setString(2, comment);
            stmt.setInt(3, ratingId);
            stmt.setInt(4, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Setzt den Kommentar auf sichtbar (Moderation)
    public boolean confirmComment(int ratingId) throws SQLException {
        String sql = "UPDATE ratings SET comment_visible = TRUE WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ratingId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Fügt einen Like hinzu. Wir fangen SQLExceptions ab, falls der User schon gelikt hat (Primary Key Violation).
    public boolean addLike(int ratingId, int userId) {
        String sql = "INSERT INTO rating_likes (rating_id, user_id) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ratingId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Ignorieren, wenn der User bereits gelikt hat
            return false;
        }
    }

    public java.util.List<Rating> getByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE user_id = ? ORDER BY created_at DESC";
        java.util.List<Rating> list = new java.util.ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Rating r = new Rating();
                r.setId(rs.getInt("id"));
                r.setMediaId(rs.getInt("media_id"));
                r.setUserId(rs.getInt("user_id"));
                r.setStars(rs.getInt("stars"));
                r.setComment(rs.getString("comment"));
                r.setCommentVisible(rs.getBoolean("comment_visible"));
                r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(r);
            }
        }
        return list;
    }
}