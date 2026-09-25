package com.example.travelshare.ui.search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.travelshare.model.Photo;
import com.example.travelshare.model.response.PhotoListResponse;
import com.example.travelshare.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends ViewModel {

    private final MutableLiveData<List<Photo>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> queryText = new MutableLiveData<>("");

    public LiveData<List<Photo>> getSearchResults() {
        return searchResults;
    }

    public LiveData<String> getQueryText() {
        return queryText;
    }

    public void performSearch(String query) {
        queryText.setValue(query);

        String q = (query == null || query.trim().isEmpty()) ? null : query.trim();

        RetrofitClient.getApiService()
                .getPhotos(q, null, null, null, null, 1, 50, null)
                .enqueue(new Callback<PhotoListResponse>() {
                    @Override
                    public void onResponse(Call<PhotoListResponse> call, Response<PhotoListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Photo> photos = response.body().getPhotos();
                            searchResults.setValue(photos != null ? photos : new ArrayList<>());
                        } else {
                            searchResults.setValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(Call<PhotoListResponse> call, Throwable t) {
                        searchResults.setValue(new ArrayList<>());
                    }
                });
    }
}
