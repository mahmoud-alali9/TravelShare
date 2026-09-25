package com.example.travelshare.network;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "TravelSharePrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_CREATED_AT = "created_at";
    private SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void saveUser(String fullName, String username, String userId) {
        prefs.edit()
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_USERNAME, username)
                .putString(KEY_USER_ID, userId)
                .apply();
    }
    
    public void saveCreatedAt(String createdAt) {
        prefs.edit().putString(KEY_CREATED_AT, createdAt).apply();
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    public String getCreatedAt() {
        return prefs.getString(KEY_CREATED_AT, null);
    }

    public String getBearerToken() {
        String token = getToken();
        return token != null ? "Bearer " + token : null;
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
