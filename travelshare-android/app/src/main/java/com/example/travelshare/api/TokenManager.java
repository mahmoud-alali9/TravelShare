package com.example.travelshare.api;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    // Nom du fichier de sauvegarde locale
    private static final String PREF_NAME    = "TravelShare";

    // Clés pour sauvegarder chaque information
    private static final String KEY_TOKEN    = "token";
    private static final String KEY_FULLNAME = "fullName";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID  = "userId";

    // SharedPreferences = système de sauvegarde locale d'Android
    // Comme un petit fichier clé/valeur sur le téléphone
    private final SharedPreferences prefs;

    // Constructeur : on initialise SharedPreferences avec le contexte
    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Sauvegarde ────────────────────────────────────────

    // Sauvegarde le token JWT reçu après connexion
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    // Sauvegarde les infos de l'utilisateur connecté
    public void saveUser(String fullName, String username, String userId) {
        prefs.edit()
                .putString(KEY_FULLNAME, fullName)
                .putString(KEY_USERNAME, username)
                .putString(KEY_USER_ID,  userId)
                .apply();
    }

    // ── Lecture ───────────────────────────────────────────

    // Récupère le token sauvegardé (null si pas connecté)
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    // Récupère le nom complet de l'utilisateur connecté
    public String getFullName() {
        return prefs.getString(KEY_FULLNAME, "");
    }

    // Récupère le username de l'utilisateur connecté
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    // Récupère l'ID de l'utilisateur connecté
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    // ── Vérification ──────────────────────────────────────

    // Vérifie si l'utilisateur est connecté
    // Retourne true si un token existe, false sinon
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    // ── Déconnexion ───────────────────────────────────────

    // Supprime toutes les données sauvegardées (déconnexion)
    public void logout() {
        prefs.edit().clear().apply();
    }

    // ── Format Bearer ─────────────────────────────────────

    // Retourne le token au format "Bearer xxx"
    // C'est le format requis par le backend pour les routes protégées
    public String getBearerToken() {
        return "Bearer " + getToken();
    }
}
