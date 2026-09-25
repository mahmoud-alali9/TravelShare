package com.example.travelshare.model.response;

import com.example.travelshare.model.Photo;
import java.util.List;

public class PhotoListResponse {

    // Nombre total de photos trouvées
    private int total;

    // Page actuelle (pour la pagination)
    private int page;

    // Nombre total de pages
    private int totalPages;

    // La liste des photos reçues du serveur
    private List<Photo> photos;

    // Getters
    public int         getTotal()      { return total; }
    public int         getPage()       { return page; }
    public int         getTotalPages() { return totalPages; }
    public List<Photo> getPhotos()     { return photos; }
}