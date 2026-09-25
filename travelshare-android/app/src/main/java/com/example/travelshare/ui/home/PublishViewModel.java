package com.example.travelshare.ui.home;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class PublishViewModel extends ViewModel {

    private final MutableLiveData<Uri> selectedImageUri = new MutableLiveData<>();
    private final MutableLiveData<List<String>> tags = new MutableLiveData<>(new ArrayList<>());
    
    // We could also store description, location, etc. here if we want them to survive rotation
    // while the user is typing. For now, let's focus on the most critical ones.

    public LiveData<Uri> getSelectedImageUri() {
        return selectedImageUri;
    }

    public void setSelectedImageUri(Uri uri) {
        selectedImageUri.setValue(uri);
    }

    public LiveData<List<String>> getTags() {
        return tags;
    }

    public void addTag(String tag) {
        List<String> currentTags = tags.getValue();
        if (currentTags != null && !currentTags.contains(tag)) {
            currentTags.add(tag);
            tags.setValue(currentTags);
        }
    }

    public void removeTag(String tag) {
        List<String> currentTags = tags.getValue();
        if (currentTags != null) {
            currentTags.remove(tag);
            tags.setValue(currentTags);
        }
    }
}
