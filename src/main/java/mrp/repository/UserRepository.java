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
}