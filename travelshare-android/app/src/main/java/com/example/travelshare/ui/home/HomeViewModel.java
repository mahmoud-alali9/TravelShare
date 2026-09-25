package com.example.travelshare.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.travelshare.model.Photo;
import com.example.travelshare.model.response.PhotoListResponse;
import com.example.travelshare.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<Photo>> photosLiveData = new MutableLiveData<>();
    private FilterCriteria currentCriteria = new FilterCriteria();
    private List<Photo> allPhotos = new ArrayList<>();

    public HomeViewModel() {
        reloadFromApi();
    }

    public LiveData<List<Photo>> getPhotos() {
        return photosLiveData;
    }

    // Appelé depuis swipe-to-refresh
    public void loadPhotos() {
        reloadFromApi();
    }

    public void setSearchQuery(String query) {
        currentCriteria.setSearchQuery(query);
        reloadFromApi();
    }

    public void filterByType(String type) {
        currentCriteria.setType(type);
        reloadFromApi();
    }

    public void updateFilterCriteria(FilterCriteria criteria) {
        criteria.setSearchQuery(currentCriteria.getSearchQuery());
        criteria.setType(currentCriteria.getType());
        this.currentCriteria = criteria;
        reloadFromApi();
    }

    // Charge depuis le backend : type + texte + géospatial
    private void reloadFromApi() {
        String q      = currentCriteria.getSearchQuery().trim().isEmpty() ? null : currentCriteria.getSearchQuery().trim();
        String typeParam = "Tous les types".equals(currentCriteria.getType()) ? null : currentCriteria.getType();
        Double lat    = currentCriteria.hasLocationFilter() ? currentCriteria.getLatitude()  : null;
        Double lng    = currentCriteria.hasLocationFilter() ? currentCriteria.getLongitude() : null;
        Double radius = currentCriteria.hasLocationFilter() ? currentCriteria.getRadius()    : null;

        RetrofitClient.getApiService()
                .getPhotos(q, typeParam, lat, lng, radius, 1, 50, null)
                .enqueue(new retrofit2.Callback<PhotoListResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<PhotoListResponse> call,
                                           retrofit2.Response<PhotoListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allPhotos = response.body().getPhotos() != null
                                    ? response.body().getPhotos() : new ArrayList<>();
                        } else {
                            allPhotos = new ArrayList<>();
                        }
                        applyLocalFilters();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<PhotoListResponse> call, Throwable t) {
                        allPhotos = new ArrayList<>();
                        applyLocalFilters();
                    }
                });
    }

    // Filtre local : auteur + date uniquement (texte et type gérés par le backend)
    private void applyLocalFilters() {
        String author = currentCriteria.getAuthor();

        List<Photo> filtered = new ArrayList<>();

        for (Photo p : allPhotos) {
            boolean matchesAuthor = "Tous les auteurs".equals(author)
                    || (p.getAuthorName() != null && p.getAuthorName().equals(author));

            boolean matchesDate = true;
            if (currentCriteria.hasDateFilter()) {
                matchesDate = isInDateRange(p.getDate(),
                        currentCriteria.getStartDate(), currentCriteria.getEndDate());
            }

            if (matchesAuthor && matchesDate) {
                filtered.add(p);
            }
        }

        photosLiveData.setValue(filtered);
    }

    private boolean isInDateRange(String photoDate, String startDate, String endDate) {
        if (photoDate == null) return false;
        String date = photoDate.length() >= 10 ? photoDate.substring(0, 10) : photoDate;
        if (startDate != null && !startDate.isEmpty() && date.compareTo(startDate) < 0) return false;
        if (endDate != null && !endDate.isEmpty()   && date.compareTo(endDate)   > 0) return false;
        return true;
    }
}
