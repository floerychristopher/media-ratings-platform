package mrp.auth;

import mrp.model.User;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TokenManager {
    // ConcurrentHashMap for thread safety
    private final Map<String, User> tokenStore = new ConcurrentHashMap<>();


    // Generates a user token and stores it
    public String generateToken(User user) {
        String token = user.getUsername() + "-mrpToken";
        tokenStore.put(token, user);
        return token;
    }

    public User getUserByToken(String token) {
        if (token == null) return null;
        return tokenStore.get(token);
    }

    // WIP: Logout
    public void removeToken(String token) {
        tokenStore.remove(token);
    }
}