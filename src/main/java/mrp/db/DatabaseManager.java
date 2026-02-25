package mrp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/mrp";
    private static final String USER = "mrp";
    private static final String PASSWORD = "mrp";

    private static DatabaseManager instance;

    private DatabaseManager() {}

    // Singleton (one manager for the whole app)
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // Returns a fresh connections each time
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // To be called once at startup
    public void initializeSchema() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                bio TEXT DEFAULT '',
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS media (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                media_type VARCHAR(50) NOT NULL,
                release_year INT,
                genre VARCHAR(255),
                age_restriction INT DEFAULT 0,
                created_by INT REFERENCES users(id),
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS ratings (
                id SERIAL PRIMARY KEY,
                media_id INT REFERENCES media(id) ON DELETE CASCADE,
                user_id INT REFERENCES users(id),
                stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
                comment TEXT,
                comment_visible BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT NOW(),
                UNIQUE(media_id, user_id)
            );

            CREATE TABLE IF NOT EXISTS rating_likes (
                rating_id INT REFERENCES ratings(id) ON DELETE CASCADE,
                user_id INT REFERENCES users(id),
                PRIMARY KEY (rating_id, user_id)
            );

            CREATE TABLE IF NOT EXISTS favorites (
                user_id INT REFERENCES users(id),
                media_id INT REFERENCES media(id) ON DELETE CASCADE,
                PRIMARY KEY (user_id, media_id)
            );
            """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database schema initialized.");
        } catch (SQLException e) {
            System.err.println("Failed to initialized schema: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
