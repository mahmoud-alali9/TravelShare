package com.example.travelshare.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.travelshare.model.Photo;
import com.example.travelshare.model.Subscription;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<List<Photo>>        photosLiveData        = new MutableLiveData<>();
    private final MutableLiveData<List<Subscription>> subscriptionsLiveData = new MutableLiveData<>();

    public LiveData<List<Photo>>        getPhotos()        { return photosLiveData; }
    public LiveData<List<Subscription>> getSubscriptions() { return subscriptionsLiveData; }

    public void loadMyPhotos() {
        RetrofitClient.getApiService().getMyPhotos().enqueue(new Callback<List<Photo>>() {
            @Override
            public void onResponse(Call<List<Photo>> call, Response<List<Photo>> response) {
                photosLiveData.setValue(
                        response.isSuccessful() && response.body() != null
                                ? response.body()
                                : new ArrayList<>()
                );
            }
            @Override
            public void onFailure(Call<List<Photo>> call, Throwable t) {
                photosLiveData.setValue(new ArrayList<>());
            }
        });
    }

    public void deletePhoto(Photo photo, OnDeleteListener listener) {
        RetrofitClient.getApiService().deletePhoto(photo.getId())
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            List<Photo> current = photosLiveData.getValue();
                            if (current != null) {
                                List<Photo> updated = new ArrayList<>(current);
                                updated.removeIf(p -> p.getId().equals(photo.getId()));
                                photosLiveData.setValue(updated);
                            }
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

    public void loadSubscriptions() {
        RetrofitClient.getApiService().getSubscriptions()
                .enqueue(new Callback<List<Subscription>>() {
                    @Override
                    public void onResponse(Call<List<Subscription>> call, Response<List<Subscription>> response) {
                        subscriptionsLiveData.setValue(
                                response.isSuccessful() && response.body() != null
                                        ? response.body()
                                        : new ArrayList<>()
                        );
                    }
                    @Override
                    public void onFailure(Call<List<Subscription>> call, Throwable t) {
                        subscriptionsLiveData.setValue(new ArrayList<>());
                    }
                });
    }

    public void unsubscribe(String type, String targetId, OnUnsubscribeListener listener) {
        RetrofitClient.getApiService().unsubscribe(type, targetId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            List<Subscription> current = subscriptionsLiveData.getValue();
                            if (current != null) {
                                List<Subscription> updated = new ArrayList<>(current);
                                updated.removeIf(s -> type.equals(s.getType()) && targetId.equals(s.getTargetId()));
                                subscriptionsLiveData.setValue(updated);
                            }
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

    public interface OnDeleteListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnUnsubscribeListener {
        void onSuccess();
        void onError(String message);
    }
}
