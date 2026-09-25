package com.example.travelshare.ui.home;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.travelshare.model.Photo;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.model.response.PhotoListResponse;
import com.example.travelshare.model.response.PhotoResponse;
import com.example.travelshare.network.RetrofitClient;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoViewModel extends ViewModel {

    private final MutableLiveData<List<Photo>> filteredPhotosLiveData = new MutableLiveData<>();
    private List<Photo>    allPhotos         = new ArrayList<>();
    private FilterCriteria currentCriteria   = new FilterCriteria();
    private boolean        lastHadSimilarity = false;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public PhotoViewModel() {
        currentCriteria.setType("Tous les types");
        currentCriteria.setAuthor("Tous les auteurs");
        currentCriteria.setSearchQuery("");
        loadPhotos();
    }

    public LiveData<List<Photo>> getPhotos() {
        return filteredPhotosLiveData;
    }

    public void loadPhotos() {
        reloadFromApi();
    }

    private void reloadFromApi() {
        String q         = currentCriteria.getSearchQuery().trim().isEmpty() ? null : currentCriteria.getSearchQuery().trim();
        String typeParam = "Tous les types".equals(currentCriteria.getType()) ? null : currentCriteria.getType();
        Double lat       = currentCriteria.hasLocationFilter() ? currentCriteria.getLatitude()  : null;
        Double lng       = currentCriteria.hasLocationFilter() ? currentCriteria.getLongitude() : null;
        Double radius    = currentCriteria.hasLocationFilter() ? currentCriteria.getRadius()    : null;

        RetrofitClient.getApiService().getPhotos(q, typeParam, lat, lng, radius, 1, 50, null)
                .enqueue(new Callback<PhotoListResponse>() {
                    @Override
                    public void onResponse(Call<PhotoListResponse> call,
                                           Response<PhotoListResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getPhotos() != null) {
                            allPhotos = new ArrayList<>(response.body().getPhotos());
                        } else {
                            allPhotos = new ArrayList<>();
                        }
                        applyFilters();
                    }

                    @Override
                    public void onFailure(Call<PhotoListResponse> call, Throwable t) {
                        allPhotos = new ArrayList<>();
                        applyFilters();
                        Log.e("PhotoViewModel", "Erreur réseau: " + t.getMessage(), t);
                    }
                });
    }

    public void deletePhoto(String photoId, OnDeleteListener listener) {
        RetrofitClient.getApiService().deletePhoto(photoId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            loadPhotos();
                            if (listener != null) listener.onSuccess();
                        } else {
                            String error = "Erreur " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    error += ": " + response.errorBody().string();
                                }
                            } catch (IOException e) { e.printStackTrace(); }
                            if (listener != null) listener.onError(error);
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        if (listener != null) listener.onError("Erreur réseau: " + t.getMessage());
                    }
                });
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onError(String message);
    }

    public void publishPhoto(Photo photo, OnPublishListener listener) {
        Map<String, Object> body = new HashMap<>();
        body.put("description", photo.getDescription());
        body.put("location", photo.getLocation());
        body.put("country", photo.getCountry());
        body.put("locationType", photo.getLocationType());
        if (photo.getLatitude() != 0.0 || photo.getLongitude() != 0.0) {
            body.put("latitude", photo.getLatitude());
            body.put("longitude", photo.getLongitude());
        }
        body.put("howToGetThere", photo.getHowToGetThere() != null ? photo.getHowToGetThere() : "");
        body.put("imageUrl", photo.getImageUrl());

        if (photo.getTags() != null) {
            body.put("tags", photo.getTags());
        }

        RetrofitClient.getApiService().publishPhoto(body)
            .enqueue(new Callback<PhotoResponse>() {
                @Override
                public void onResponse(Call<PhotoResponse> call, Response<PhotoResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        loadPhotos();
                        if (listener != null) listener.onSuccess(response.body().getPhoto());
                    } else {
                        if (listener != null) listener.onError("Erreur lors de la publication (" + response.code() + ")");
                    }
                }

                @Override
                public void onFailure(Call<PhotoResponse> call, Throwable t) {
                    if (listener != null) listener.onError("Erreur réseau : " + t.getMessage());
                }
            });
    }

    public interface OnPublishListener {
        void onSuccess(Photo photo);
        void onError(String message);
    }

    public void updateFilterCriteria(FilterCriteria criteria) {
        if (criteria == null) return;
        // Préserver type et searchQuery gérés séparément
        criteria.setSearchQuery(currentCriteria.getSearchQuery());
        criteria.setType(currentCriteria.getType());
        this.currentCriteria = criteria;
        String simId = criteria.getSimilarPhotoId();
        if (simId != null && !simId.isEmpty()) {
            lastHadSimilarity = true;
            loadSimilarPhotos(simId);
        } else {
            lastHadSimilarity = false;
            reloadFromApi();
        }
    }

    private void loadSimilarPhotos(String simId) {
        RetrofitClient.getApiService().getPhotos(null, null, null, null, null, 1, 100, simId)
                .enqueue(new Callback<PhotoListResponse>() {
                    @Override
                    public void onResponse(Call<PhotoListResponse> call,
                                           Response<PhotoListResponse> response) {
                        allPhotos = (response.isSuccessful() && response.body() != null
                                && response.body().getPhotos() != null)
                                ? new ArrayList<>(response.body().getPhotos())
                                : new ArrayList<>();
                        applyFilters();
                    }
                    @Override
                    public void onFailure(Call<PhotoListResponse> call, Throwable t) {
                        allPhotos = new ArrayList<>();
                        applyFilters();
                    }
                });
    }

    public void filterByType(String type) {
        if (type != null) {
            currentCriteria.setType(type);
            reloadFromApi();
        }
    }

    public void setSearchQuery(String query) {
        currentCriteria.setSearchQuery(query != null ? query : "");
        reloadFromApi();
    }

    // Filtre local : auteur + date uniquement (type, texte, géospatial → backend)
    private void applyFilters() {
        List<Photo> results = new ArrayList<>();

        for (Photo p : allPhotos) {
            if (currentCriteria.getAuthor() != null &&
                    !currentCriteria.getAuthor().equals("Tous les auteurs") &&
                    !currentCriteria.getAuthor().equals(p.getAuthorName())) {
                continue;
            }

            if (currentCriteria.hasDateFilter()) {
                if (!isDateInRange(p.getDate(), currentCriteria.getStartDate(), currentCriteria.getEndDate())) {
                    continue;
                }
            }

            results.add(p);
        }
        filteredPhotosLiveData.setValue(results);
    }

    private boolean isDateInRange(String photoDateStr, String startStr, String endStr) {
        if (photoDateStr == null || photoDateStr.isEmpty()) return false;
        try {
            String cleanPhotoDate = photoDateStr.length() > 10 ? photoDateStr.substring(0, 10) : photoDateStr;
            Date photoDate = sdf.parse(cleanPhotoDate);

            if (startStr != null && !startStr.isEmpty()) {
                Date startDate = sdf.parse(startStr);
                if (photoDate.before(startDate)) return false;
            }

            if (endStr != null && !endStr.isEmpty()) {
                Date endDate = sdf.parse(endStr);
                if (photoDate.after(endDate)) return false;
            }

            return true;
        } catch (ParseException e) {
            return true;
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
