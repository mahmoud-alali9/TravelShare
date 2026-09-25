package com.example.travelshare.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelshare.MainActivity;
import com.example.travelshare.MainViewModel;
import com.example.travelshare.R;
import com.example.travelshare.model.response.AuthResponse;
import com.example.travelshare.network.RetrofitClient;
import com.example.travelshare.network.TokenManager;
import com.example.travelshare.ui.home.PhotoAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private MainViewModel    mainViewModel;
    private ProfileViewModel profileViewModel;
    private TokenManager     tokenManager;
    private PhotoAdapter     photoAdapter;

    private View       layoutLoggedOut, layoutLoggedIn;
    private Button     btnLogin, btnLogout, btnMySubscriptions;
    private TextView   tvName, tvInitial, tvMemberSince, tvPhotosCount, tvNoPhotos;
    private RecyclerView rvMyPhotos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainViewModel    = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        tokenManager     = new TokenManager(requireContext());

        layoutLoggedOut   = view.findViewById(R.id.layout_profile_logged_out);
        layoutLoggedIn    = view.findViewById(R.id.layout_profile_logged_in);
        btnLogin          = view.findViewById(R.id.btn_profile_login);
        btnLogout         = view.findViewById(R.id.btn_logout);
        btnMySubscriptions = view.findViewById(R.id.btn_my_subscriptions);
        tvName            = view.findViewById(R.id.tv_profile_name);
        tvInitial         = view.findViewById(R.id.tv_profile_initial);
        tvMemberSince     = view.findViewById(R.id.tv_profile_member_since);
        tvPhotosCount     = view.findViewById(R.id.tv_my_photos_count);
        tvNoPhotos        = view.findViewById(R.id.tv_no_photos);
        rvMyPhotos        = view.findViewById(R.id.rv_my_photos);

        setupPhotoAdapter();
        setupObservers();
        setupListeners();
    }

    private void setupPhotoAdapter() {
        photoAdapter = new PhotoAdapter(requireContext(), PhotoAdapter.VIEW_GRID);
        photoAdapter.setLoggedIn(true);
        rvMyPhotos.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvMyPhotos.setAdapter(photoAdapter);

        photoAdapter.setOnPhotoDeleteListener(photo ->
            new AlertDialog.Builder(requireContext())
                .setTitle("Supprimer la photo")
                .setMessage("Voulez-vous vraiment supprimer cette photo ?")
                .setPositiveButton("Supprimer", (d, w) -> {
                    String token = tokenManager.getToken();
                    if (token == null) return;
                    profileViewModel.deletePhoto(photo, new ProfileViewModel.OnDeleteListener() {
                        @Override public void onSuccess() {
                            if (getContext() != null)
                                Toast.makeText(getContext(), "Photo supprimée", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onError(String message) {
                            if (getContext() != null)
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show()
        );
    }

    private void setupObservers() {
        mainViewModel.isUserLoggedIn().observe(getViewLifecycleOwner(), loggedIn -> {
            if (loggedIn) {
                layoutLoggedOut.setVisibility(View.GONE);
                layoutLoggedIn.setVisibility(View.VISIBLE);
                fetchUserProfile();
                profileViewModel.loadMyPhotos();
            } else {
                layoutLoggedOut.setVisibility(View.VISIBLE);
                layoutLoggedIn.setVisibility(View.GONE);
            }
        });

        profileViewModel.getPhotos().observe(getViewLifecycleOwner(), photos -> {
            if (photos == null || photos.isEmpty()) {
                tvNoPhotos.setVisibility(View.VISIBLE);
                rvMyPhotos.setVisibility(View.GONE);
                tvPhotosCount.setText("(0)");
            } else {
                tvNoPhotos.setVisibility(View.GONE);
                rvMyPhotos.setVisibility(View.VISIBLE);
                tvPhotosCount.setText("(" + photos.size() + ")");
                photoAdapter.setCurrentUserId(tokenManager.getUserId());
                photoAdapter.submitList(photos);
            }
        });
    }

    private void fetchUserProfile() {
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                    AuthResponse.UserData user = response.body().getUser();
                    updateUI(user.getFullName(), user.getUsername(), user.getCreatedAt());
                    tokenManager.saveUser(user.getFullName(), user.getUsername(), user.getId());
                    tokenManager.saveCreatedAt(user.getCreatedAt());
                } else {
                    Log.e(TAG, "Erreur fetchProfile: " + response.code());
                    loadLocalData();
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "Échec fetchProfile", t);
                loadLocalData();
            }
        });
    }

    private void loadLocalData() {
        updateUI(tokenManager.getFullName(), tokenManager.getUsername(), tokenManager.getCreatedAt());
    }

    private void updateUI(String fullName, String username, String createdAt) {
        if (fullName != null && !fullName.isEmpty()) {
            tvName.setText(fullName);
            tvInitial.setText(fullName.substring(0, 1).toUpperCase());
        } else if (username != null && !username.isEmpty()) {
            tvName.setText(username);
            tvInitial.setText(username.substring(0, 1).toUpperCase());
        } else {
            tvName.setText("Utilisateur Voyageur");
            tvInitial.setText("U");
        }

        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = sdfInput.parse(createdAt);
                if (date != null) {
                    String year = new SimpleDateFormat("yyyy", Locale.FRANCE).format(date);
                    tvMemberSince.setText("Membre depuis " + year);
                }
            } catch (ParseException e) {
                try {
                    tvMemberSince.setText("Membre depuis " + createdAt.substring(0, 4));
                } catch (Exception ex) {
                    tvMemberSince.setText("Membre depuis...");
                }
            }
        } else {
            tvMemberSince.setText("Membre depuis...");
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showLoginDialog();
            }
        });

        btnLogout.setOnClickListener(v -> {
            tokenManager.clear();
            mainViewModel.logout();
            Toast.makeText(getContext(), "Déconnecté", Toast.LENGTH_SHORT).show();
        });

        btnMySubscriptions.setOnClickListener(v ->
            new SubscriptionsBottomSheet().show(getChildFragmentManager(), "subscriptions")
        );
    }
}
