package com.example.travelshare.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Photo {

    @SerializedName("_id")
    private String mongoId;

    private String imageUrl;
    private Author author;
    private String location;
    private String country;
    private String locationType;
    private double latitude;
    private double longitude;
    private String description;

    @SerializedName("createdAt")
    private String date;

    private String howToGetThere;

    @SerializedName("likes")
    private List<String> likesArray;
    private int serverLikeCount = -1;

    @SerializedName("comments")
    private List<Object> commentsArray;

    private boolean likedByMe;
    private boolean isPublic;
    private List<String> tags;

    public Photo() {
    }

    public static class Author {
        @SerializedName("_id")
        private String id;
        private String fullName;
        private String username;
        private String avatar;

        public Author() {}

        public String getId()       { return id; }
        public String getFullName() { return fullName; }
        public String getUsername() { return username; }
        public String getAvatar()   { return avatar; }
    }

    public String getId()             { return mongoId; }
    public String getImageUrl()       { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Author getAuthor()         { return author; }

    public String getAuthorName() {
        return author != null ? author.getFullName() : "";
    }
    public String getAuthorUsername() {
        return author != null ? author.getUsername() : "";
    }
    public String getAuthorAvatar() {
        return author != null ? author.getAvatar() : "";
    }

    public String  getLocation()     { return location; }
    public void setLocation(String location) { this.location = location; }

    public String  getCountry()      { return country; }
    public void setCountry(String country) { this.country = country; }

    public String  getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public double  getLatitude()     { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double  getLongitude()    { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String  getDescription()  { return description; }
    public void setDescription(String description) { this.description = description; }

    public String  getDate()         { return date; }
    public String  getHowToGetThere(){ return howToGetThere; }
    public void setHowToGetThere(String howToGetThere) { this.howToGetThere = howToGetThere; }

    public int getLikeCount() {
        return serverLikeCount >= 0 ? serverLikeCount : (likesArray != null ? likesArray.size() : 0);
    }

    public void setLikeCount(int count) { this.serverLikeCount = count; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }

    public int getCommentCount() {
        return commentsArray != null ? commentsArray.size() : 0;
    }

    public boolean isLikedByMe()  { return likedByMe; }
    public boolean isPublic()     { return isPublic; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public void toggleLike() {
        this.likedByMe = !this.likedByMe;
        if (likesArray == null) {
            likesArray = new ArrayList<>();
        }
        if (likedByMe) {
            likesArray.add("placeholder_user_id");
        } else if (!likesArray.isEmpty()) {
            likesArray.remove(0);
        }
    }
}
