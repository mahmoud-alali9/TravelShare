package com.example.travelshare.ui.home;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.travelshare.model.Notification;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.model.response.NotificationListResponse;
import com.example.travelshare.model.response.UnreadCountResponse;
import com.example.travelshare.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationViewModel extends ViewModel {

    private final MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCountLiveData = new MutableLiveData<>();

    public LiveData<List<Notification>> getNotifications() {
        return notificationsLiveData;
    }

    public LiveData<Integer> getUnreadCount() {
        return unreadCountLiveData;
    }

    public void loadNotifications() {
        RetrofitClient.getApiService().getNotifications()
                .enqueue(new Callback<NotificationListResponse>() {
                    @Override
                    public void onResponse(Call<NotificationListResponse> call, Response<NotificationListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            notificationsLiveData.setValue(response.body().getNotifications());
                        } else {
                            Log.e("NotificationVM", "Erreur loadNotifications: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<NotificationListResponse> call, Throwable t) {
                        Log.e("NotificationVM", "Echec réseau loadNotifications: " + t.getMessage());
                    }
                });
    }

    public void loadUnreadCount() {
        RetrofitClient.getApiService().getUnreadCount()
                .enqueue(new Callback<UnreadCountResponse>() {
                    @Override
                    public void onResponse(Call<UnreadCountResponse> call, Response<UnreadCountResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            unreadCountLiveData.setValue(response.body().getCount());
                        }
                    }

                    @Override
                    public void onFailure(Call<UnreadCountResponse> call, Throwable t) {
                        Log.e("NotificationVM", "Echec réseau unreadCount: " + t.getMessage());
                    }
                });
    }

    public void markAsRead(Notification notification) {
        if (notification.isRead()) return;

        notification.setRead(true);
        List<Notification> currentList = notificationsLiveData.getValue();
        if (currentList != null) {
            notificationsLiveData.setValue(new ArrayList<>(currentList));
        }

        int currentUnread = unreadCountLiveData.getValue() != null ? unreadCountLiveData.getValue() : 0;
        if (currentUnread > 0) {
            unreadCountLiveData.setValue(currentUnread - 1);
        }

        RetrofitClient.getApiService().markAsRead(notification.getId())
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (!response.isSuccessful()) {
                            Log.e("NotificationVM", "Erreur markAsRead: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Log.e("NotificationVM", "Echec réseau markAsRead: " + t.getMessage());
                    }
                });
    }
}
