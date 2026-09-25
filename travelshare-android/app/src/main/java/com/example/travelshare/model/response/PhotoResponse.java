package com.example.travelshare.model.response;

import com.example.travelshare.model.Photo;

public class PhotoResponse {
    private String message;
    private Photo photo;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }
}
