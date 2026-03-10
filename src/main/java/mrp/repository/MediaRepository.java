package mrp.repository;

import mrp.db.DatabaseManager;
import mrp.model.Media;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MediaRepository {
    private final DatabaseManager db;

    public MediaRepository(DatabaseManager db) {
        this.db = db;
    }

    public Media create(Media media) throws SQLException {
        String sql = "INSERT INTO media (title, description, media_type, release_year, genre, age_restriction, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        // Connection und Statement im try-Block, damit beide geschlossen werden
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, media.getTitle());
            stmt.setString(2, media.getDescription());
            stmt.setString(3, media.getMediaType());
            stmt.setInt(4, media.getReleaseYear());

            // Liste in getrennten String umwandeln ("sci-fi,thriller")
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

        //getrennten String aus der DB wieder in eine Liste umwandeln
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

    // --- Favoriten ---

    public boolean addFavorite(int mediaId, int userId) {
        String sql = "INSERT INTO favorites (user_id, media_id) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, mediaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Ignorieren, wenn es schon in den Favoriten ist (Primary Key)
            return false;
        }
    }

    public boolean removeFavorite(int mediaId, int userId) {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND media_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, mediaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Media> getFavoritesByUserId(int userId) throws SQLException {
        //JOIN, um direkt alle Media-Daten der Favoriten zu bekommen
        String sql = "SELECT m.* FROM media m JOIN favorites f ON m.id = f.media_id WHERE f.user_id = ?";
        List<Media> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs)); // mapRow existiert hier schon praktischerweise!
            }
        }
        return list;
    }

    // --- Suche und Filter ---

    public List<Media> searchAndFilter(Map<String, String> params) throws SQLException {
        // grundquery mit JOIN für den Durchschnittsscore
        StringBuilder sql = new StringBuilder(
                "SELECT m.*, COALESCE(AVG(r.stars), 0) AS avg_score " +
                        "FROM media m " +
                        "LEFT JOIN ratings r ON m.id = r.media_id " +
                        "WHERE 1=1 " // 1=1 ist ein Trick, damit wir alle folgenden Filter mit "AND ..." anhängen können
        );

        List<Object> values = new ArrayList<>();

        // 1. Filter (WHERE)
        if (params.containsKey("title") && !params.get("title").isBlank()) {
            sql.append("AND m.title ILIKE ? "); // ILIKE ignoriert Groß-/Kleinschreibung
            values.add("%" + params.get("title") + "%");
        }
        if (params.containsKey("genre") && !params.get("genre").isBlank()) {
            sql.append("AND m.genre ILIKE ? ");
            values.add("%" + params.get("genre") + "%");
        }
        if (params.containsKey("mediaType") && !params.get("mediaType").isBlank()) {
            sql.append("AND m.media_type = ? ");
            values.add(params.get("mediaType"));
        }
        if (params.containsKey("releaseYear") && !params.get("releaseYear").isBlank()) {
            sql.append("AND m.release_year = ? ");
            values.add(Integer.parseInt(params.get("releaseYear")));
        }
        if (params.containsKey("ageRestriction") && !params.get("ageRestriction").isBlank()) {
            sql.append("AND m.age_restriction <= ? ");
            values.add(Integer.parseInt(params.get("ageRestriction")));
        }

        // 2. Gruppierung (notwendig wegen JOINs und der AVG-Funktion)
        sql.append("GROUP BY m.id ");

        // 3. Rating-Filter (HAVING)
        if (params.containsKey("rating") && !params.get("rating").isBlank()) {
            sql.append("HAVING COALESCE(AVG(r.stars), 0) >= ? ");
            values.add(Double.parseDouble(params.get("rating")));
        }

        // 4. Sortierung (ORDER BY)
        String sortBy = params.getOrDefault("sortBy", "id");
        if (sortBy.equals("title")) {
            sql.append("ORDER BY m.title ASC ");
        } else if (sortBy.equals("year")) {
            sql.append("ORDER BY m.release_year DESC ");
        } else if (sortBy.equals("score")) {
            sql.append("ORDER BY avg_score DESC ");
        } else {
            sql.append("ORDER BY m.id DESC "); // Standard Sortierung
        }

        // --- Ausführung ---
        List<Media> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Werte dynamisch in das Statement einsetzen
            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Media m = mapRow(rs); // Bisheriges Mapping nutzen
                m.setAverageScore(rs.getDouble("avg_score")); // berechneten Score ergänzen
                list.add(m);
            }
        }
        return list;
    }
}