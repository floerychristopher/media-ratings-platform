package mrp.controller;

import mrp.model.User;
import mrp.server.HttpRequest;
import mrp.server.HttpResponse;
import mrp.service.UserService;
import mrp.util.JsonUtil;

import java.sql.SQLException;
import java.util.List;

public class LeaderboardController {
    private final UserService userService;

    public LeaderboardController(UserService userService) {
        this.userService = userService;
    }

    //GET /api/leaderboard
    public HttpResponse getLeaderboard(HttpRequest req) {
        try {
            List<User> leaderboard = userService.getLeaderboard();
            return HttpResponse.ok(JsonUtil.toJson(leaderboard));
        } catch (SQLException e) {
            return HttpResponse.internalError("Database error: " + e.getMessage());
        }
    }
}