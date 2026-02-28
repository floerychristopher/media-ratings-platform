package mrp.auth;

import mrp.model.User;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TokenManager {
    // token string → User who owns it
    // ConcurrentHashMap because multiple threads will access this simultaneously
    private final Map<String, User> tokenStore = new ConcurrentHashMap<>();

    /**
     * Generates a token for a user and stores it.
     * Per the spec: "mustermann-mrpToken"
     */
    public String generateToken(User user) {
        String token = user.getUsername() + "-mrpToken";
        tokenStore.put(token, user);
        return token;
    }

    /**
     * Looks up who owns this token.
     * Returns null if the token is invalid/expired.
     */
    public User getUserByToken(String token) {
        if (token == null) return null;
        return tokenStore.get(token);
    }

    /**
     * Removes a token (for logout).
     */
    public void removeToken(String token) {
        tokenStore.remove(token);
    }
}