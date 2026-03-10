package mrp.service;

import mrp.auth.TokenManager;
import mrp.model.User;
import mrp.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Map;

public class UserService {
    private final UserRepository userRepository;
    private final TokenManager tokenManager;

    public UserService(UserRepository userRepository, TokenManager tokenManager) {
        this.userRepository = userRepository;
        this.tokenManager = tokenManager;
    }

    /**
     * Registers a new user.
     * Returns the created user (without password), or throws if validation fails.
     */
    public User register(String username, String password) throws Exception {
        // --- Validation ---
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Check if username already taken
        User existing = userRepository.findByUsername(username);
        if (existing != null) {
            throw new IllegalStateException("Username already exists");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User user = new User(username, hashedPassword);
        return userRepository.create(user);
    }

    /**
     * Logs in a user.
     * Returns the token string, or throws if credentials are wrong.
     */
    public String login(String username, String password) throws Exception {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password required");
        }

        User user = userRepository.findByUsername(username);
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return tokenManager.generateToken(user);
    }

    /**
     * Gets a user's profile info.
     */
    public User getProfile(String username) throws SQLException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        // NEU: Lade die Statistiken in das User-Objekt, bevor es zurückgegeben wird!
        userRepository.loadUserStatistics(user);
        return user;
    }

    /**
     * Updates profile. Only the user themselves can do this.
     */
    public User updateProfile(User authenticatedUser, String username, String bio) throws Exception {
        // Check: you can only edit YOUR profile
        if (!authenticatedUser.getUsername().equals(username)) {
            throw new SecurityException("Cannot edit another user's profile");
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setBio(bio);
        userRepository.update(user);
        return user;
    }

    public java.util.List<User> getLeaderboard() throws java.sql.SQLException {
        return userRepository.getLeaderboard();
    }
}