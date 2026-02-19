package mrp.db;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/mrp";
    private static final String USR = "mrp";
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
}
