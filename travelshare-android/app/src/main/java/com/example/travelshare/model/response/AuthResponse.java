package com.example.travelshare.model.response;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    // Le token JWT reçu après connexion ou inscription
    private String token;

    // Message du serveur (ex: "Connexion réussie")
    private String message;

    // Les infos de l'utilisateur connecté
    private UserData user;

    // Getters
    public String   getToken()   { return token; }
    public String   getMessage() { return message; }
    public UserData getUser()    { return user; }

    // Classe interne qui représente les données de l'utilisateur
    public static class UserData {

        // ID unique de l'utilisateur dans MongoDB
        @SerializedName("_id")
        private String id;

        // Nom complet ex: "Jean Dupont"
        private String fullName;

        // Nom d'utilisateur ex: "jean_explorer"
        private String username;

        // Email de l'utilisateur
        private String email;

        // URL de l'avatar (photo de profil)
        private String avatar;

        private String createdAt;

        // Getters
        public String getId()       { return id; }
        public String getFullName() { return fullName; }
        public String getUsername() { return username; }
        public String getEmail()    { return email; }
        public String getAvatar()   { return avatar; }
        public String getCreatedAt(){ return createdAt; }
    }
}