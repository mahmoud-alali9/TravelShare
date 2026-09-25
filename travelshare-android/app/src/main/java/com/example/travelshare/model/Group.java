package com.example.travelshare.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Group {

    @SerializedName("_id")
    private String id;

    private String name;
    private String description;

    // Le backend retourne "coverImage" pas "coverImageUrl"
    @SerializedName("coverImage")
    private String coverImageUrl;

    // Les membres sont des objets pas des strings
    private List<Member> members;

    private Creator creator;

    private String createdAt;

    public Group() {}

    // ── Getters ──────────────────────────────────────────
    public String getId()           { return id; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public String getCoverImageUrl(){ return coverImageUrl; }
    public List<Member> getMembers(){ return members; }
    public Creator getCreator()     { return creator; }
    public String getCreatedAt()    { return createdAt; }

    // Nombre de membres calculé depuis la liste
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }

    // ── Setters ──────────────────────────────────────────
    public void setId(String id)                    { this.id = id; }
    public void setName(String name)                { this.name = name; }
    public void setDescription(String description)  { this.description = description; }
    public void setCoverImageUrl(String url)        { this.coverImageUrl = url; }
    public void setMembers(List<Member> members)    { this.members = members; }

    // ── Classe interne Member ─────────────────────────────
    public static class Member {
        @SerializedName("_id")
        private String id;
        private String fullName;
        private String username;
        private String avatar;

        public String getId()       { return id; }
        public String getFullName() { return fullName; }
        public String getUsername() { return username; }
        public String getAvatar()   { return avatar; }
    }

    // ── Classe interne Creator ────────────────────────────
    public static class Creator {
        @SerializedName("_id")
        private String id;
        private String fullName;
        private String username;

        public String getId()       { return id; }
        public String getFullName() { return fullName; }
        public String getUsername() { return username; }
    }
}