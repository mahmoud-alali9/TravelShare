package com.example.travelshare.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelshare.R;
import com.example.travelshare.model.Photo;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.model.response.PhotoListResponse;
import com.example.travelshare.network.RetrofitClient;
import com.example.travelshare.network.TokenManager;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupPhotosActivity extends AppCompatActivity {

    private static final String TAG = "GroupPhotosActivity";
    private RecyclerView recycler;
    private PhotoAdapter adapter;
    private String groupId;
    private String groupName;
    private TokenManager tokenManager;
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_photos);

        tokenManager = new TokenManager(this);
        groupId = getIntent().getStringExtra("group_id");
        groupName = getIntent().getStringExtra("group_name");
        String groupDesc = getIntent().getStringExtra("group_desc");
        String groupCover = getIntent().getStringExtra("group_cover");
        layoutEmpty = findViewById(R.id.layout_empty_group);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(groupName);

        ImageView ivHeader = findViewById(R.id.iv_group_header);
        TextView tvDesc = findViewById(R.id.tv_group_desc);
        tvDesc.setText(groupDesc);

        Glide.with(this)
                .load(groupCover)
                .placeholder(R.drawable.placeholder_photo)
                .centerCrop()
                .into(ivHeader);

        setupRecyclerView();

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_photo_to_group);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(GroupPhotosActivity.this, SelectUserPhotosActivity.class);
            intent.putExtra("group_id", groupId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroupPhotos();
    }

    private void setupRecyclerView() {
        recycler = findViewById(R.id.recycler_group_photos);
        adapter = new PhotoAdapter(this, PhotoAdapter.VIEW_GRID);
        adapter.setLoggedIn(true);
        adapter.setCurrentUserId(tokenManager.getUserId());
        
        // Gérer le retrait de la photo du groupe
        adapter.setOnPhotoDeleteListener(photo -> {
            new AlertDialog.Builder(this)
                    .setTitle("Retirer du groupe")
                    .setMessage("Voulez-vous retirer cette photo de ce groupe ?")
                    .setPositiveButton("Retirer", (dialog, which) -> removePhotoFromGroup(photo.getId()))
                    .setNegativeButton("Annuler", null)
                    .show();
        });

        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        recycler.setAdapter(adapter);
    }

    private void removePhotoFromGroup(String photoId) {
        if (groupId == null || photoId == null) return;

        RetrofitClient.getApiService().removePhotoFromGroup(groupId, photoId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(GroupPhotosActivity.this, "Photo retirée du groupe", Toast.LENGTH_SHORT).show();
                            loadGroupPhotos(); // Recharger la liste
                        } else {
                            Toast.makeText(GroupPhotosActivity.this, "Erreur lors du retrait", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Toast.makeText(GroupPhotosActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadGroupPhotos() {
        if (groupId == null) return;

        Log.d(TAG, "Chargement des photos du groupe : " + groupId);
        RetrofitClient.getApiService().getGroupPhotos(groupId)
                .enqueue(new Callback<PhotoListResponse>() {
                    @Override
                    public void onResponse(Call<PhotoListResponse> call, Response<PhotoListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Photo> photos = response.body().getPhotos();
                            if (photos == null) photos = new ArrayList<>();
                            
                            Log.d(TAG, "Nombre de photos reçues pour le groupe : " + photos.size());
                            
                            // On passe une nouvelle liste pour forcer DiffUtil à se mettre à jour
                            adapter.submitList(new ArrayList<>(photos));
                            
                            if (layoutEmpty != null) {
                                layoutEmpty.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        } else {
                            Log.e(TAG, "Erreur API : " + response.code());
                            Toast.makeText(GroupPhotosActivity.this, "Erreur de chargement", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<PhotoListResponse> call, Throwable t) {
                        Log.e(TAG, "Échec réseau", t);
                        Toast.makeText(GroupPhotosActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
