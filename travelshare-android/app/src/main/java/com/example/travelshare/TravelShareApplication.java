package com.example.travelshare;

import android.app.Application;
import com.example.travelshare.network.RetrofitClient;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class TravelShareApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialisation de Retrofit avec le contexte
        RetrofitClient.init(this);
        
        // Initialisation de Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dlm0vdf8i");
        config.put("secure", "true");
        MediaManager.init(this, config);
    }
}
