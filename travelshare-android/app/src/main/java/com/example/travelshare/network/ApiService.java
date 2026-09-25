package com.example.travelshare.network;

import com.example.travelshare.model.Photo;
import com.example.travelshare.model.Group;
import com.example.travelshare.model.Comment;
import com.example.travelshare.model.Subscription;
import com.example.travelshare.model.response.AuthResponse;
import com.example.travelshare.model.response.LikeResponse;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.model.response.NotificationListResponse;
import com.example.travelshare.model.response.PhotoListResponse;
import com.example.travelshare.model.response.PhotoResponse;
import com.example.travelshare.model.response.UnreadCountResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── PHOTOS ───────────────────────────────────────────
    @GET("api/photos")
    Call<PhotoListResponse> getPhotos(
            @Query("query") String query,
            @Query("type") String type,
            @Query("lat") Double lat,
            @Query("lng") Double lng,
            @Query("radius") Double radius,
            @Query("page") Integer page,
            @Query("limit") Integer limit,
            @Query("similarPhotoId") String similarPhotoId
    );

    @GET("api/photos/{id}")
    Call<Photo> getPhotoById(@Path("id") String photoId);

    @Multipart
    @POST("api/photos/upload")
    Call<Map<String, String>> uploadPhoto(
            @Part MultipartBody.Part file
    );

    @POST("api/photos")
    Call<PhotoResponse> publishPhoto(
            @Body Map<String, Object> body
    );

    @GET("api/photos/me")
    Call<List<Photo>> getMyPhotos();

    @DELETE("api/photos/{id}")
    Call<MessageResponse> deletePhoto(
            @Path("id") String photoId
    );

    @POST("api/photos/{id}/report")
    Call<MessageResponse> reportPhoto(
            @Path("id") String photoId,
            @Body Map<String, String> body
    );

    @POST("api/photos/{id}/toggle-like")
    Call<LikeResponse> toggleLike(
            @Path("id") String photoId
    );

    // ── COMMENTAIRES ─────────────────────────────────────
    @GET("api/photos/{id}/comments")
    Call<Comment.CommentsResponse> getComments(@Path("id") String photoId);

    @POST("api/photos/{id}/comments")
    Call<Comment> addComment(
            @Path("id") String photoId,
            @Body Comment body
    );

    @DELETE("api/photos/{photoId}/comments/{commentId}")
    Call<Void> deleteComment(
            @Path("photoId") String photoId,
            @Path("commentId") String commentId
    );

    // ── GROUPES ──────────────────────────────────────────
    @GET("api/groups")
    Call<List<Group>> getGroups();

    @GET("api/groups/discover")
    Call<List<Group>> getDiscoverGroups();

    @POST("api/groups")
    Call<Group> createGroup(
            @Body Map<String, String> body
    );

    @PUT("api/groups/{id}/join")
    Call<MessageResponse> joinGroup(
            @Path("id") String groupId
    );

    @PUT("api/groups/{id}/leave")
    Call<MessageResponse> leaveGroup(
            @Path("id") String groupId
    );

    @DELETE("api/groups/{id}")
    Call<MessageResponse> deleteGroup(
            @Path("id") String groupId
    );

    @GET("api/groups/{id}/photos")
    Call<PhotoListResponse> getGroupPhotos(
            @Path("id") String groupId
    );

    @POST("api/groups/{id}/photos")
    Call<MessageResponse> addPhotoToGroup(
            @Path("id") String groupId,
            @Body Map<String, Object> body
    );

    @DELETE("api/groups/{groupId}/photos/{photoId}")
    Call<MessageResponse> removePhotoFromGroup(
            @Path("groupId") String groupId,
            @Path("photoId") String photoId
    );

    // ── AUTH & NOTIFS ────────────────────────────────────
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body Map<String, String> body);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body Map<String, String> body);

    @GET("api/auth/me")
    Call<AuthResponse> getProfile();

    @PUT("api/auth/fcm-token")
    Call<Void> updateFcmToken(
            @Body Map<String, String> body
    );

    @GET("api/notifications")
    Call<NotificationListResponse> getNotifications();

    @GET("api/notifications/unread-count")
    Call<UnreadCountResponse> getUnreadCount();

    @PATCH("api/notifications/{id}/read")
    Call<MessageResponse> markAsRead(
            @Path("id") String notificationId
    );

    // ── ABONNEMENTS ──────────────────────────────────────
    @POST("api/subscriptions")
    Call<MessageResponse> subscribe(
            @Body Map<String, String> body
    );

    @GET("api/subscriptions")
    Call<List<Subscription>> getSubscriptions();

    @DELETE("api/subscriptions/by")
    Call<MessageResponse> unsubscribe(
            @Query("type") String type,
            @Query("targetId") String targetId
    );
}
