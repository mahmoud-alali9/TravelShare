package com.example.travelshare.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelshare.R;
import com.example.travelshare.model.Photo;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.model.response.PhotoListResponse;
import com.example.travelshare.network.RetrofitClient;
import com.example.travelshare.network.TokenManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectUserPhotosActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG_PHOTO";
    private RecyclerView recycler;
    private PhotoAdapter adapter;
    private TokenManager tokenManager;
    private String groupId;
    private LinearLayout layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user_photos);

        groupId = getIntent().getStringExtra("group_id");
        tokenManager = new TokenManager(this);
        layoutEmpty = findViewById(R.id.layout_empty_state);

        Toolbar toolbar = findViewById(R.id.toolbar_select_photos);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Choisir mes photos");
        }

        setupRecyclerView();
        loadAllPhotosAndFilter();
    }

    private void setupRecyclerView() {
        recycler = findViewById(R.id.recycler_select_user_photos);
        adapter = new PhotoAdapter(this, PhotoAdapter.VIEW_GRID);
        adapter.setLoggedIn(true); 
        
        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        recycler.setAdapter(adapter);

        recycler.addOnItemTouchListener(new RecyclerItemClickListener(this, recycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                List<Photo> currentList = adapter.getCurrentList();
                if (position >= 0 && position < currentList.size()) {
                    addPhotoToGroup(currentList.get(position));
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {}
        }));
    }

    private void loadAllPhotosAndFilter() {
        String currentUserId = tokenManager.getUserId();
        String currentUsername = tokenManager.getUsername();

        Log.d(TAG, "Moi (App): ID=" + currentUserId + " | Username=" + currentUsername);

        if (currentUserId == null && currentUsername == null) {
            Log.e(TAG, "ERREUR: Toutes les infos utilisateur sont NULL");
            return;
        }

        RetrofitClient.getApiService().getPhotos(null, null, null, null, null, 1, 100, null)
                .enqueue(new Callback<PhotoListResponse>() {
                    @Override
                    public void onResponse(Call<PhotoListResponse> call, Response<PhotoListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Photo> allPhotos = response.body().getPhotos();
                            List<Photo> myPhotos = new ArrayList<>();
                            
                            if (allPhotos != null) {
                                for (Photo p : allPhotos) {
                                    boolean isMine = false;

                                    if (p.getAuthor() != null) {
                                        String authorId = p.getAuthor().getId();
                                        String authorUsername = p.getAuthor().getUsername();
                                        
                                        Log.d(TAG, "Analyse Photo " + p.getId());
                                        Log.d(TAG, " > Auteur ID: " + authorId + " | Username: " + authorUsername);

                                        // Comparaison par ID
                                        if (currentUserId != null && currentUserId.equals(authorId)) {
                                            isMine = true;
                                        }
                                        // Comparaison par Pseudo (fallback)
                                        else if (currentUsername != null && authorUsername != null
                                                 && currentUsername.equalsIgnoreCase(authorUsername)) {
                                            isMine = true;
                                        }
                                    }

                                    if (isMine) {
                                        Log.d(TAG, "MATCH !");
                                        myPhotos.add(p);
                                    }
                                }
                            }
                            displayPhotos(myPhotos);
                        }
                    }

                    @Override
                    public void onFailure(Call<PhotoListResponse> call, Throwable t) {
                        Toast.makeText(SelectUserPhotosActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayPhotos(List<Photo> photos) {
        adapter.submitList(photos);
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void addPhotoToGroup(Photo photo) {
        if (groupId == null) return;

        Map<String, Object> body = new HashMap<>();
        body.put("photoId", photo.getId());

        RetrofitClient.getApiService().addPhotoToGroup(groupId, body)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(SelectUserPhotosActivity.this, "Photo ajoutée !", Toast.LENGTH_SHORT).show();
                            finish(); 
                        } else {
                            String errorMsg = "Erreur lors de l'ajout (" + response.code() + ")";
                            try {
                                if (response.errorBody() != null) {
                                    errorMsg += ": " + response.errorBody().string();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Log.e(TAG, errorMsg);
                            Toast.makeText(SelectUserPhotosActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Toast.makeText(SelectUserPhotosActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
