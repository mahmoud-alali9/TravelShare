package com.example.travelshare.ui.detail;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.travelshare.R;
import com.example.travelshare.network.RetrofitClient;
import com.github.chrisbanes.photoview.PhotoView;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
        setContentView(R.layout.activity_full_screen_image);

        PhotoView photoView = findViewById(R.id.photo_view);
        ImageButton btnClose = findViewById(R.id.btn_close_full);

        String imageUrl = getIntent().getStringExtra("image_url");
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullImageUrl = imageUrl;
            
            if (!imageUrl.startsWith("http") && !imageUrl.startsWith("content") && !imageUrl.startsWith("file")) {
                String baseUrl = RetrofitClient.getBaseUrl();
                String cleanPath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
                fullImageUrl = baseUrl + cleanPath;
            }

            Log.d("FullScreenImage", "Chargement de l'URL : " + fullImageUrl);

            Glide.with(this)
                    .load(fullImageUrl)
                    .into(photoView);
        } else {
            Toast.makeText(this, "URL d'image invalide", Toast.LENGTH_SHORT).show();
        }

        btnClose.setOnClickListener(v -> finish());
    }
}
