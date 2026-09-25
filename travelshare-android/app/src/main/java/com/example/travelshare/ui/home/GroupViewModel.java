package com.example.travelshare.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.travelshare.model.Group;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.network.RetrofitClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupViewModel extends AndroidViewModel {

    private static final String TAG = "GroupViewModel";
    private final MutableLiveData<List<Group>> groupsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Group>> discoverGroupsLiveData = new MutableLiveData<>();

    public GroupViewModel(Application application) {
        super(application);
    }

    public LiveData<List<Group>> getGroups() {
        return groupsLiveData;
    }

    public LiveData<List<Group>> getDiscoverGroups() {
        return discoverGroupsLiveData;
    }

    public void loadGroups() {
        RetrofitClient.getApiService().getGroups()
                .enqueue(new Callback<List<Group>>() {
                    @Override
                    public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            groupsLiveData.setValue(response.body());
                        } else {
                            groupsLiveData.setValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Group>> call, Throwable t) {
                        groupsLiveData.setValue(new ArrayList<>());
                    }
                });
    }

    public void loadDiscoverGroups() {
        RetrofitClient.getApiService().getDiscoverGroups()
                .enqueue(new Callback<List<Group>>() {
                    @Override
                    public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            discoverGroupsLiveData.setValue(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Group>> call, Throwable t) {
                        Log.e(TAG, "loadDiscoverGroups: Failure", t);
                    }
                });
    }

    public void joinGroup(String groupId, OnActionListener listener) {
        RetrofitClient.getApiService().joinGroup(groupId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            loadGroups();
                            loadDiscoverGroups();
                            if (listener != null) listener.onSuccess();
                        } else {
                            handleError(response, listener);
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Log.e(TAG, "joinGroup: Failure", t);
                        if (listener != null) listener.onError("Erreur réseau : " + t.getMessage());
                    }
                });
    }

    public void leaveGroup(String groupId, OnActionListener listener) {
        RetrofitClient.getApiService().leaveGroup(groupId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            loadGroups();
                            loadDiscoverGroups();
                            if (listener != null) listener.onSuccess();
                        } else {
                            handleError(response, listener);
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        if (listener != null) listener.onError("Erreur réseau : " + t.getMessage());
                    }
                });
    }

    public void deleteGroup(String groupId, OnActionListener listener) {
        RetrofitClient.getApiService().deleteGroup(groupId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            loadGroups();
                            loadDiscoverGroups();
                            if (listener != null) listener.onSuccess();
                        } else {
                            handleError(response, listener);
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        if (listener != null) listener.onError("Erreur réseau : " + t.getMessage());
                    }
                });
    }

    private void handleError(Response<?> response, OnActionListener listener) {
        String error = "Erreur " + response.code();
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                Log.e(TAG, "Détail erreur : " + errorBody);
                error += " : " + errorBody;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (listener != null) listener.onError(error);
    }

    public void createNewGroup(String name, String desc, String coverUrl) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("description", desc);
        body.put("coverImage", coverUrl);

        RetrofitClient.getApiService().createGroup(body)
                .enqueue(new Callback<Group>() {
                    @Override
                    public void onResponse(Call<Group> call, Response<Group> response) {
                        if (response.isSuccessful()) {
                            loadGroups();
                        }
                    }

                    @Override
                    public void onFailure(Call<Group> call, Throwable t) {
                        Log.e(TAG, "createNewGroup: Failure", t);
                    }
                });
    }

    public interface OnActionListener {
        void onSuccess();
        void onError(String message);
    }
}
