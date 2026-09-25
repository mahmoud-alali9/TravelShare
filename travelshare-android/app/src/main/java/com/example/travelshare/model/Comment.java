package com.example.travelshare.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Comment {
    private String _id;
    private String text;
    private String createdAt;
    private CommentUser user;

    public Comment() {}

    public Comment(String text) {
        this.text = text;
    }

    public Comment(String id, String text, String createdAt, CommentUser user) {
        this._id = id;
        this.text = text;
        this.createdAt = createdAt;
        this.user = user;
    }

    public String getId() { return _id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public CommentUser getUser() { return user; }
    public void setUser(CommentUser user) { this.user = user; }

    public static class CommentUser {
        private String fullName;
        private String username;
        private String avatar;

        public CommentUser() {}

        public CommentUser(String fullName, String username, String avatar) {
            this.fullName = fullName;
            this.username = username;
            this.avatar = avatar;
        }

        public String getFullName() { return fullName; }
        public String getUsername() { return username; }
        public String getAvatar() { return avatar; }
    }

    public static class CommentsResponse {
        private int total;
        private List<Comment> comments;

        public int getTotal() { return total; }
        public List<Comment> getComments() { return comments; }
    }
}
