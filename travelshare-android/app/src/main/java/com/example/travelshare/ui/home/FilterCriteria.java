package com.example.travelshare.ui.home;

public class FilterCriteria {
    private String author = "Tous les auteurs";
    private String type = "Tous les types";
    private String searchQuery = "";
    private Double latitude = null;
    private Double longitude = null;
    private Double radius = null;
    private String startDate = "";
    private String endDate = "";
    private String similarPhotoId = "";

    public FilterCriteria() {}

    // Getters et Setters
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getRadius() { return radius; }
    public void setRadius(Double radius) { this.radius = radius; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getSimilarPhotoId() { return similarPhotoId; }
    public void setSimilarPhotoId(String similarPhotoId) { this.similarPhotoId = similarPhotoId; }

    public boolean hasLocationFilter() {
        return latitude != null && longitude != null && radius != null;
    }

    public boolean hasDateFilter() {
        return (startDate != null && !startDate.isEmpty()) || (endDate != null && !endDate.isEmpty());
    }
}
