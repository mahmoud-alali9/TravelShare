package com.example.travelshare.model.response;

public class LikeResponse {

    // true = l'utilisateur a liké, false = il a unliké
    private boolean liked;

    // Nombre total de likes après l'action
    private int likeCount;

    // Getters
    public boolean isLiked()      { return liked; }
    public int     getLikeCount() { return likeCount; }
}