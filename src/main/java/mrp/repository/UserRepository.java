package mrp.repository;

import mrp.db.DatabaseManager;
import mrp.model.User;

import java.sql.*;

public class UserRepository {
    private final DatabaseManager db;

    public UserRepository(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Inserts a new user into the database.
     * Returns the user with the generated ID set, or null if the username already exists.
     */
    public User create(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?) RETURNING id, created_at";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user.setId(rs.getInt("id"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return user;
            }
            return null;
        }
        // If username already exists, PostgreSQL throws a unique constraint violation.
        // We let that SQLException bubble up — the service layer catches it.
    }

    /**
     * Finds a user by username. Returns null if not found.
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    /**
     * Find user by ID.
     */
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    /**
     * Updates a user's profile fields (bio for now).
     */
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET bio = ? WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getBio());
            stmt.setInt(2, user.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Converts a ResultSet row into a User object.
     * This is called from every query — keeps the mapping in one place.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setBio(rs.getString("bio"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    }

    // Füge diese Methode unten im UserRepository hinzu:

    public java.util.List<User> getLeaderboard() throws SQLException {
        // LEFT JOIN sorgt dafür, dass auch Nutzer mit 0 Ratings im Leaderboard auftauchen
        String sql = "SELECT u.id, u.username, u.bio, u.created_at, " +
                "COUNT(r.id) AS rating_count " +
                "FROM users u " +
                "LEFT JOIN ratings r ON u.id = r.user_id " +
                "GROUP BY u.id " +
                "ORDER BY rating_count DESC, u.username ASC";

        java.util.List<User> leaderboard = new java.util.ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setBio(rs.getString("bio"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                user.setRatingCount(rs.getInt("rating_count"));
                // user.setPassword() wird absichtlich weggelassen!

                leaderboard.add(user);
            }
        }
        return leaderboard;
    }

    /**
     * Lädt die Statistiken (Anzahl, Durchschnitt, Lieblingsgenre) für einen User.
     */
    public void loadUserStatistics(User user) throws SQLException {
        // 1. Anzahl und Durchschnitt berechnen
        String sqlStats = "SELECT COUNT(id) AS total, COALESCE(AVG(stars), 0) AS avg_score FROM ratings WHERE user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlStats)) {
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user.setRatingCount(rs.getInt("total"));
                user.setAverageScore(rs.getDouble("avg_score"));
            }
        }

        // 2. Lieblingsgenre berechnen (Das am häufigsten bewertete Genre)
        String sqlGenre = "SELECT trim(unnested_genre) AS genre, COUNT(*) as count " +
                "FROM ratings r " +
                "JOIN media m ON r.media_id = m.id " +
                "CROSS JOIN LATERAL unnest(string_to_array(m.genre, ',')) AS unnested_genre " +
                "WHERE r.user_id = ? AND unnested_genre != '' " +
                "GROUP BY genre " +
                "ORDER BY count DESC LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlGenre)) {
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user.setFavoriteGenre(rs.getString("genre"));
            } else {
                user.setFavoriteGenre("-"); // Falls er noch nichts bewertet hat
            }
        }
    }
}