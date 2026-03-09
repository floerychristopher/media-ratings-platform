package mrp.repository;

import mrp.db.DatabaseManager;
import mrp.model.Media;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MediaRepository {
    private final DatabaseManager db;

    public MediaRepository(DatabaseManager db) {
        this.db = db;
    }

    public Media create(Media media) throws SQLException {
        String sql = "INSERT INTO media (title, description, media_type, release_year, genre, age_restriction, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        // WICHTIG: Connection und Statement im try-Block deklarieren, damit beide geschlossen werden!
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, media.getTitle());
            stmt.setString(2, media.getDescription());
            stmt.setString(3, media.getMediaType());
            stmt.setInt(4, media.getReleaseYear());

            // Liste in kommagetrennten String umwandeln ("sci-fi,thriller")
            String genreString = media.getGenres() != null ? String.join(",", media.getGenres()) : "";
            stmt.setString(5, genreString);

            stmt.setInt(6, media.getAgeRestriction());
            stmt.setInt(7, media.getCreatedBy());
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                media.setId(rs.getInt("id"));
                media.setCreatedAt(LocalDateTime.now());
            }
        }
        return media;
    }

    public Media getById(int id) throws SQLException {
        String sql = "SELECT * FROM media WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<Media> getAll() throws SQLException {
        String sql = "SELECT * FROM media ORDER BY id DESC";
        List<Media> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean update(Media media) throws SQLException {
        String sql = "UPDATE media SET title=?, description=?, media_type=?, release_year=?, genre=?, age_restriction=? WHERE id=? AND created_by=?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, media.getTitle());
            stmt.setString(2, media.getDescription());
            stmt.setString(3, media.getMediaType());
            stmt.setInt(4, media.getReleaseYear());

            String genreString = media.getGenres() != null ? String.join(",", media.getGenres()) : "";
            stmt.setString(5, genreString);

            stmt.setInt(6, media.getAgeRestriction());
            stmt.setInt(7, media.getId());
            stmt.setInt(8, media.getCreatedBy());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int mediaId, int userId) throws SQLException {
        String sql = "DELETE FROM media WHERE id=? AND created_by=?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mediaId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Media mapRow(ResultSet rs) throws SQLException {
        Media m = new Media();
        m.setId(rs.getInt("id"));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        m.setMediaType(rs.getString("media_type"));
        m.setReleaseYear(rs.getInt("release_year"));

        // Kommagetrennten String aus der DB wieder in eine Liste umwandeln
        String genreString = rs.getString("genre");
        if (genreString != null && !genreString.isEmpty()) {
            m.setGenres(Arrays.asList(genreString.split(",")));
        } else {
            m.setGenres(new ArrayList<>());
        }

        m.setAgeRestriction(rs.getInt("age_restriction"));
        m.setCreatedBy(rs.getInt("created_by"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) m.setCreatedAt(createdAt.toLocalDateTime());

        return m;
    }
}