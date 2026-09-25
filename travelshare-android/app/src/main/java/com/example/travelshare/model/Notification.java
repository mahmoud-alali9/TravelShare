package com.example.travelshare.model;

import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("_id")
    private String id;
    private String type;        // "new_photo", "new_like", "group_photo"
    private String title;
    private String message;
    
    @SerializedName(value = "date", alternate = {"createdAt"})
    private String date;
    
    private boolean isRead;

    private String relatedId;
    private String photo;
    private String group;

    public Notification() {}

    public Notification(String id, String type, String title, String message, String date, boolean isRead) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.date = date;
        this.isRead = isRead;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public String getRelatedId() {
        if (relatedId != null && !relatedId.isEmpty()) return relatedId;
        if (photo != null && !photo.isEmpty()) return photo;
        if (group != null && !group.isEmpty()) return group;
        return "";
    }

    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
}
