package com.example.travelshare;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.travelshare.model.response.UnreadCountResponse;
import com.example.travelshare.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<Boolean> userLoggedIn = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> unreadNotificationsCount = new MutableLiveData<>(0);

    public LiveData<Boolean> isUserLoggedIn() {
        return userLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        userLoggedIn.setValue(loggedIn);
    }

    public LiveData<Integer> getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void refreshNotificationCount() {
        RetrofitClient.getApiService().getUnreadCount()
                .enqueue(new Callback<UnreadCountResponse>() {
                    @Override
                    public void onResponse(Call<UnreadCountResponse> call, Response<UnreadCountResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            unreadNotificationsCount.setValue(response.body().getCount());
                        }
                    }

                    @Override
                    public void onFailure(Call<UnreadCountResponse> call, Throwable t) {
                        // Keep current value
                    }
                });
    }

    public void logout() {
        userLoggedIn.setValue(false);
        unreadNotificationsCount.setValue(0);
    }
}
