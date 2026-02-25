package mrp.model;

import java.time.LocalDateTime;

public class Rating {
    private int id;
    private int mediaId;
    private int userId;
    private int stars;
    private String comment;
    private boolean commentVisible;
    private LocalDateTime createdAt;
    private int likeCount;         // not stored in DB — calculated on read
    private String username;       // not stored — joined from users table for display

    public Rating() {}

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMediaId() { return mediaId; }
    public void setMediaId(int mediaId) { this.mediaId = mediaId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public boolean isCommentVisible() { return commentVisible; }
    public void setCommentVisible(boolean commentVisible) { this.commentVisible = commentVisible; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}