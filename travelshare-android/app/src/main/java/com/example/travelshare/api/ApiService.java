package com.example.travelshare.api;

import com.example.travelshare.model.response.AuthResponse;
import com.example.travelshare.model.response.PhotoListResponse;
import com.example.travelshare.model.response.LikeResponse;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.model.response.NotificationListResponse;
import com.example.travelshare.model.response.UnreadCountResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── Authentification ──────────────────────────────────

    // Inscription : envoie fullName, username, email, password
    // Reçoit un token JWT + infos utilisateur
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body Map<String, String> body);

    // Connexion : envoie email, password
    // Reçoit un token JWT + infos utilisateur
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body Map<String, String> body);

    // ── Photos ────────────────────────────────────────────

    // Récupère la liste des photos avec filtres optionnels
    // query = recherche texte, type = type de lieu
    // lat/lng/radius = recherche géospatiale
    // page/limit = pagination
    @GET("api/photos")
    Call<PhotoListResponse> getPhotos(
            @Query("query")  String query,
            @Query("type")   String type,
            @Query("lat")    Double lat,
            @Query("lng")    Double lng,
            @Query("radius") Double radius,
            @Query("page")   int page,
            @Query("limit")  int limit
    );

    // Récupère des photos aléatoires pour le flux découverte
    @GET("api/photos/random")
    Call<PhotoListResponse> getRandomPhotos();

    // Publie une nouvelle photo (token obligatoire)
    @POST("api/photos")
    Call<MessageResponse> createPhoto(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    // Like ou unlike une photo (token obligatoire)
    @POST("api/photos/{id}/toggle-like")
    Call<LikeResponse> toggleLike(
            @Header("Authorization") String token,
            @Path("id") String photoId
    );

    // Signale une photo (token obligatoire)
    @POST("api/photos/{id}/report")
    Call<MessageResponse> reportPhoto(
            @Header("Authorization") String token,
            @Path("id") String photoId,
            @Body Map<String, String> body
    );

    // Ajoute un commentaire sur une photo (token obligatoire)
    @POST("api/photos/{id}/comments")
    Call<MessageResponse> addComment(
            @Header("Authorization") String token,
            @Path("id") String photoId,
            @Body Map<String, String> body
    );

    // Supprime un commentaire (token obligatoire)
    @DELETE("api/photos/{id}/comments/{commentId}")
    Call<MessageResponse> deleteComment(
            @Header("Authorization") String token,
            @Path("id") String photoId,
            @Path("commentId") String commentId
    );

    // ── Notifications ─────────────────────────────────────

    // Récupère toutes les notifications de l'utilisateur connecté
    @GET("api/notifications")
    Call<NotificationListResponse> getNotifications(
            @Header("Authorization") String token
    );

    // Récupère le nombre de notifications non lues
    // Utilisé pour afficher le badge sur l'icône cloche
    @GET("api/notifications/unread-count")
    Call<UnreadCountResponse> getUnreadCount(
            @Header("Authorization") String token
    );

    // Marque une notification comme lue
    @PATCH("api/notifications/{id}/read")
    Call<MessageResponse> markAsRead(
            @Header("Authorization") String token,
            @Path("id") String notificationId
    );
}
