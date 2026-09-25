package com.example.travelshare.ui.detail;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.travelshare.model.Comment;
import com.example.travelshare.model.Photo;
import com.example.travelshare.model.Subscription;
import com.example.travelshare.model.response.LikeResponse;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoDetailViewModel extends ViewModel {

    private final MutableLiveData<Photo>   photoLiveData    = new MutableLiveData<>();
    private final MutableLiveData<List<Comment>> commentsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSubscribed     = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public LiveData<Photo>        getPhoto()        { return photoLiveData; }
    public LiveData<List<Comment>> getComments()   { return commentsLiveData; }
    public LiveData<Boolean>      getIsSubscribed() { return isSubscribed; }

    public void loadPhoto(String photoId) {
        if (photoId == null) return;

        RetrofitClient.getApiService().getPhotoById(photoId).enqueue(new Callback<Photo>() {
            @Override
            public void onResponse(Call<Photo> call, Response<Photo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    photoLiveData.setValue(response.body());
                } else {
                    photoLiveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Photo> call, Throwable t) {
                photoLiveData.setValue(null);
            }
        });
    }

    public void loadComments(String photoId) {
        if (photoId == null) return;

        RetrofitClient.getApiService().getComments(photoId).enqueue(new Callback<Comment.CommentsResponse>() {
            @Override
            public void onResponse(Call<Comment.CommentsResponse> call, Response<Comment.CommentsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentsLiveData.setValue(response.body().getComments());
                } else {
                    commentsLiveData.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<Comment.CommentsResponse> call, Throwable t) {
                commentsLiveData.setValue(new ArrayList<>());
            }
        });
    }

    public void toggleLike() {
        Photo photo = photoLiveData.getValue();
        if (photo == null) return;

        RetrofitClient.getApiService().toggleLike(photo.getId()).enqueue(new Callback<LikeResponse>() {
            @Override
            public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    photo.setLikedByMe(response.body().isLiked());
                    photo.setLikeCount(response.body().getLikeCount());
                    photoLiveData.setValue(photo);
                }
            }

            @Override
            public void onFailure(Call<LikeResponse> call, Throwable t) {}
        });
    }

    public void postComment(String photoId, String text, OnCommentAddedListener listener) {
        if (photoId == null || text.isEmpty()) return;

        Comment commentRequest = new Comment(text);
        RetrofitClient.getApiService().addComment(photoId, commentRequest).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    loadComments(photoId);
                    if (listener != null) listener.onSuccess();
                } else {
                    if (listener != null) listener.onError("Erreur lors de l'ajout");
                }
            }

            @Override
            public void onFailure(Call<Comment> call, Throwable t) {
                if (listener != null) listener.onError("Erreur réseau");
            }
        });
    }

    public void deleteComment(String photoId, String commentId, OnCommentDeletedListener listener) {
        if (photoId == null || commentId == null) return;

        RetrofitClient.getApiService().deleteComment(photoId, commentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadComments(photoId);
                    if (listener != null) listener.onSuccess();
                } else {
                    if (listener != null) listener.onError("Impossible de supprimer ce commentaire");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (listener != null) listener.onError("Erreur réseau");
            }
        });
    }

    public void checkSubscription(String authorId) {
        if (authorId == null) return;
        RetrofitClient.getApiService().getSubscriptions()
                .enqueue(new Callback<List<Subscription>>() {
                    @Override
                    public void onResponse(Call<List<Subscription>> call, Response<List<Subscription>> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        boolean found = false;
                        for (Subscription s : response.body()) {
                            if ("author".equals(s.getType()) && authorId.equals(s.getTargetId())) {
                                found = true;
                                break;
                            }
                        }
                        isSubscribed.setValue(found);
                    }
                    @Override
                    public void onFailure(Call<List<Subscription>> call, Throwable t) {}
                });
    }

    public void toggleSubscription(String authorId) {
        Boolean current = isSubscribed.getValue();
        if (current == null || authorId == null) return;
        if (current) {
            RetrofitClient.getApiService().unsubscribe("author", authorId)
                    .enqueue(new Callback<MessageResponse>() {
                        @Override
                        public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                            if (response.isSuccessful()) isSubscribed.setValue(false);
                        }
                        @Override public void onFailure(Call<MessageResponse> call, Throwable t) {}
                    });
        } else {
            Map<String, String> body = new HashMap<>();
            body.put("type", "author");
            body.put("targetId", authorId);
            RetrofitClient.getApiService().subscribe(body)
                    .enqueue(new Callback<MessageResponse>() {
                        @Override
                        public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                            if (response.isSuccessful()) isSubscribed.setValue(true);
                        }
                        @Override public void onFailure(Call<MessageResponse> call, Throwable t) {}
                    });
        }
    }

    public void reportPhoto(OnReportListener listener) {
        Photo photo = photoLiveData.getValue();
        if (photo == null) return;
        Map<String, String> body = new HashMap<>();
        body.put("reason", "Contenu inapproprié");
        RetrofitClient.getApiService().reportPhoto(photo.getId(), body)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            if (listener != null) listener.onSuccess();
                        } else {
                            if (listener != null) listener.onError("Erreur " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        if (listener != null) listener.onError("Erreur réseau");
                    }
                });
    }

    public interface OnReportListener {
        void onSuccess();
        void onError(String message);
    }

    public void scheduleAiTagsRefresh(String photoId) {
        handler.postDelayed(() -> loadPhoto(photoId), 2500);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacksAndMessages(null);
    }

    public interface OnCommentAddedListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnCommentDeletedListener {
        void onSuccess();
        void onError(String message);
    }
}
