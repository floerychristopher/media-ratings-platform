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

     //Registers user
     //Returns created user (without password)
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

    // Login, returns token string
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

    // get profile information
    public User getProfile(String username) throws SQLException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.loadUserStatistics(user);
        return user;
    }

    // Update (only the user itself)
    public User updateProfile(User authUser, String targetUsername, String newBio, String newEmail) throws SQLException {
        if (!authUser.getUsername().equals(targetUsername)) {
            throw new SecurityException("Cannot edit another user's profile");
        }

        User targetUser = userRepository.findByUsername(targetUsername);
        if (targetUser == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Setze Bio und E-Mail
        targetUser.setBio(newBio);
        targetUser.setEmail(newEmail);

        userRepository.update(targetUser); // Speichert in der DB
        userRepository.loadUserStatistics(targetUser);

        return targetUser;
    }

    public java.util.List<User> getLeaderboard() throws java.sql.SQLException {
        return userRepository.getLeaderboard();
    }
}