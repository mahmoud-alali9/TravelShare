package com.example.travelshare.ui.detail;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelshare.R;
import com.example.travelshare.model.Comment;
import com.example.travelshare.model.Photo;
import com.example.travelshare.network.RetrofitClient;
import com.example.travelshare.network.TokenManager;
import com.example.travelshare.ui.home.CommentAdapter;

import java.util.ArrayList;

public class PhotoDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_ID = "photo_id";
    public static final String EXTRA_NEW_PHOTO = "new_photo";

    private PhotoDetailViewModel viewModel;
    private TokenManager tokenManager;
    private CommentAdapter commentAdapter;

    private ImageView   imgDetail;
    private TextView    tvAvatar, tvAuthor, tvDate, tvType,
                        tvLocation, tvDescription, tvHowTo, tvLikeCount, tvLoginMsg, tvCommentsTitle;
    private ImageButton btnLike, btnShare, btnReport, btnSendComment;
    private Button      btnOpenMaps, btnSubscribeAuthor;
    private RecyclerView rvComments;
    private EditText    etComment;
    private LinearLayout layoutAddComment;
    private ChipGroup   chipGroupTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        viewModel = new ViewModelProvider(this).get(PhotoDetailViewModel.class);
        tokenManager = new TokenManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        bindViews();
        setupRecyclerView();
        setupObservers();
        setupActions();

        String photoId = getIntent().getStringExtra(EXTRA_PHOTO_ID);
        if (photoId != null) {
            viewModel.loadPhoto(photoId);
            viewModel.loadComments(photoId);
            if (getIntent().getBooleanExtra(EXTRA_NEW_PHOTO, false)) {
                viewModel.scheduleAiTagsRefresh(photoId);
            }
        } else {
            finish();
        }
    }

    private void bindViews() {
        imgDetail     = findViewById(R.id.img_detail);
        tvAvatar      = findViewById(R.id.tv_detail_avatar);
        tvAuthor      = findViewById(R.id.tv_detail_author);
        tvDate        = findViewById(R.id.tv_detail_date);
        tvType        = findViewById(R.id.tv_detail_type);
        tvLocation    = findViewById(R.id.tv_detail_location);
        tvDescription = findViewById(R.id.tv_detail_description);
        tvHowTo       = findViewById(R.id.tv_detail_how_to);
        tvLikeCount   = findViewById(R.id.tv_detail_like_count);
        btnLike       = findViewById(R.id.btn_detail_like);
        btnShare      = findViewById(R.id.btn_detail_share);
        btnReport     = findViewById(R.id.btn_detail_report);
        btnOpenMaps   = findViewById(R.id.btn_open_maps);
        
        // Commentaires
        rvComments    = findViewById(R.id.rv_detail_comments);
        etComment     = findViewById(R.id.et_detail_comment);
        btnSendComment = findViewById(R.id.btn_detail_send_comment);
        layoutAddComment = findViewById(R.id.layout_detail_add_comment);
        tvLoginMsg         = findViewById(R.id.tv_detail_login_to_comment);
        tvCommentsTitle    = findViewById(R.id.tv_detail_comments_title);
        chipGroupTags      = findViewById(R.id.chip_group_detail_tags);
        btnSubscribeAuthor = findViewById(R.id.btn_subscribe_author);
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter();
        commentAdapter.setCurrentUsername(tokenManager.getUsername());
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        commentAdapter.setDeleteListener(comment -> {
            Photo photo = viewModel.getPhoto().getValue();
            if (tokenManager.isLoggedIn() && photo != null) {
                viewModel.deleteComment(photo.getId(), comment.getId(), new PhotoDetailViewModel.OnCommentDeletedListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(PhotoDetailActivity.this, "Commentaire supprimé", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(PhotoDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateCommentsTitle(int count) {
        tvCommentsTitle.setText("Commentaires (" + count + ")");
    }

    private void setupObservers() {
        viewModel.getPhoto().observe(this, photo -> {
            if (photo != null) {
                updateUI(photo);
                // Bouton abonnement : visible si connecté et photo d'un autre utilisateur
                String myId = tokenManager.getUserId();
                boolean isMyPhoto = myId != null && photo.getAuthor() != null
                        && myId.equals(photo.getAuthor().getId());
                if (tokenManager.isLoggedIn() && !isMyPhoto && photo.getAuthor() != null) {
                    btnSubscribeAuthor.setVisibility(View.VISIBLE);
                    viewModel.checkSubscription(photo.getAuthor().getId());
                } else {
                    btnSubscribeAuthor.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getIsSubscribed().observe(this, subscribed -> {
            if (subscribed == null) return;
            if (subscribed) {
                btnSubscribeAuthor.setText("Abonné ✓");
                btnSubscribeAuthor.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            } else {
                btnSubscribeAuthor.setText("S'abonner");
                btnSubscribeAuthor.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#7C3AED")));
            }
        });

        viewModel.getComments().observe(this, comments -> {
            if (comments != null) {
                commentAdapter.setComments(new ArrayList<>(comments));
                updateCommentsTitle(comments.size());
            }
        });
        
        boolean isLoggedIn = tokenManager.isLoggedIn();
        layoutAddComment.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        tvLoginMsg.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
    }

    private String getFullUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.startsWith("http") || url.startsWith("content")) return url;
        String cleanPath = url.startsWith("/") ? url.substring(1) : url;
        return RetrofitClient.getBaseUrl() + cleanPath;
    }

    private void updateUI(Photo photo) {
        String imageUrl = getFullUrl(photo.getImageUrl());
        
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_photo)
                .centerCrop()
                .into(imgDetail);

        // Action pour voir en plein écran
        imgDetail.setOnClickListener(v -> {
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putExtra("image_url", imageUrl);
            startActivity(intent);
        });

        tvAuthor.setText(photo.getAuthorName());
        tvDate.setText(photo.getDate());
        String initial = photo.getAuthorName().length() > 0
                ? String.valueOf(photo.getAuthorName().charAt(0)).toUpperCase()
                : "?";
        tvAvatar.setText(initial);

        tvLocation.setText(photo.getLocation() + ", " + photo.getCountry());
        tvType.setText(photo.getLocationType());
        tvDescription.setText(photo.getDescription());
        tvHowTo.setText(photo.getHowToGetThere());

        java.util.List<String> tags = photo.getTags();
        if (tags != null && !tags.isEmpty()) {
            chipGroupTags.removeAllViews();
            for (String tag : tags) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setClickable(false);
                chipGroupTags.addView(chip);
            }
            chipGroupTags.setVisibility(View.VISIBLE);
        } else {
            chipGroupTags.setVisibility(View.GONE);
        }

        tvLikeCount.setText(String.valueOf(photo.getLikeCount()));
        if (photo.isLikedByMe()) {
            btnLike.setImageResource(R.drawable.ic_favorite);
            btnLike.setColorFilter(Color.parseColor("#E53935"));
        } else {
            btnLike.setImageResource(R.drawable.ic_favorite_border);
            btnLike.setColorFilter(Color.parseColor("#BDBDBD"));
        }
    }

    private void setupActions() {
        btnLike.setOnClickListener(v -> {
            if (tokenManager.isLoggedIn()) {
                viewModel.toggleLike();
            } else {
                Toast.makeText(this, "Veuillez vous connecter pour aimer", Toast.LENGTH_SHORT).show();
            }
        });

        btnShare.setOnClickListener(v -> {
            Photo photo = viewModel.getPhoto().getValue();
            if (photo == null) return;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Découvrez " + photo.getLocation() + " sur TravelShare !\n" + getFullUrl(photo.getImageUrl()));
            startActivity(Intent.createChooser(shareIntent, "Partager via…"));
        });

        btnReport.setOnClickListener(v -> showReportDialog());
        btnOpenMaps.setOnClickListener(v -> openMaps());

        btnSubscribeAuthor.setOnClickListener(v -> {
            Photo photo = viewModel.getPhoto().getValue();
            if (tokenManager.isLoggedIn() && photo != null && photo.getAuthor() != null) {
                viewModel.toggleSubscription(photo.getAuthor().getId());
            }
        });

        btnSendComment.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (text.isEmpty()) return;

            Photo photo = viewModel.getPhoto().getValue();

            if (tokenManager.isLoggedIn() && photo != null) {
                viewModel.postComment(photo.getId(), text, new PhotoDetailViewModel.OnCommentAddedListener() {
                    @Override
                    public void onSuccess() {
                        etComment.setText("");
                        Toast.makeText(PhotoDetailActivity.this, "Commentaire ajouté", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(PhotoDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void openMaps() {
        Photo photo = viewModel.getPhoto().getValue();
        if (photo == null) return;
        Uri gmmUri = Uri.parse("geo:" + photo.getLatitude() + "," + photo.getLongitude() + "?q=" + photo.getLatitude() + "," + photo.getLongitude() + "(" + Uri.encode(photo.getLocation()) + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + photo.getLatitude() + "," + photo.getLongitude());
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private void showReportDialog() {
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Connectez-vous pour signaler", Toast.LENGTH_SHORT).show();
            return;
        }
        Photo photo = viewModel.getPhoto().getValue();
        if (photo == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Signaler cette photo")
                .setMessage("Signaler la photo de " + photo.getAuthorName()
                        + " pour contenu inapproprié ?")
                .setPositiveButton("Signaler", (dialog, which) ->
                        viewModel.reportPhoto(new PhotoDetailViewModel.OnReportListener() {
                            @Override public void onSuccess() {
                                Toast.makeText(PhotoDetailActivity.this,
                                        "Signalement envoyé", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(String message) {
                                Toast.makeText(PhotoDetailActivity.this,
                                        message, Toast.LENGTH_SHORT).show();
                            }
                        })
                )
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
