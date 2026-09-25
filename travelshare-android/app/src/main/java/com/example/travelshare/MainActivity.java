package com.example.travelshare;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.travelshare.network.TokenManager;
import com.example.travelshare.ui.auth.AuthDialog;
import com.example.travelshare.ui.home.HomeFragment;
import com.example.travelshare.ui.home.NotificationsBottomSheet;
import com.example.travelshare.ui.profile.ProfileFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private TokenManager tokenManager;

    private ImageView    btnLogin;
    private LinearLayout layoutUserConnected;
    private TextView     tvUserInitial;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);

        // Liaison des vues
        btnLogin            = findViewById(R.id.btn_login);
        layoutUserConnected = findViewById(R.id.layout_user_connected);
        tvUserInitial       = findViewById(R.id.tv_user_initial);
        bottomNav           = findViewById(R.id.bottom_nav);

        // Initialisation du ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupObservers();
        setupListeners();

        // Navigation par défaut
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        setupBottomNavigation();
        
        // Check initial login state
        if (tokenManager.isLoggedIn()) {
            viewModel.setLoggedIn(true);
            viewModel.refreshNotificationCount();
        }
    }

    private void setupObservers() {
        // Observe l'état de connexion
        viewModel.isUserLoggedIn().observe(this, loggedIn -> {
            if (loggedIn) {
                btnLogin.setVisibility(View.GONE);
                layoutUserConnected.setVisibility(View.VISIBLE);
                
                String display = "U";
                String fullName = tokenManager.getFullName();
                String username = tokenManager.getUsername();
                
                if (fullName != null && !fullName.isEmpty()) {
                    display = fullName.substring(0, 1).toUpperCase();
                } else if (username != null && !username.isEmpty()) {
                    display = username.substring(0, 1).toUpperCase();
                }
                
                tvUserInitial.setText(display);
                viewModel.refreshNotificationCount();
            } else {
                btnLogin.setVisibility(View.VISIBLE);
                layoutUserConnected.setVisibility(View.GONE);
            }
        });

        // Observe le badge de notifications sur la barre du bas
        viewModel.getUnreadNotificationsCount().observe(this, count -> {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_notifications);
            if (count != null && count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
            } else {
                badge.setVisible(false);
            }
        });
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> showLoginDialog());

        layoutUserConnected.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Déconnexion")
                    .setMessage("Voulez-vous vous déconnecter ?")
                    .setPositiveButton("Déconnecter", (dialog, which) -> {
                        tokenManager.clear();
                        viewModel.logout();
                        Toast.makeText(this, "Déconnecté", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_notifications) {
                showNotifications();
                return true;
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void showNotifications() {
        NotificationsBottomSheet sheet = new NotificationsBottomSheet(() -> {
            viewModel.refreshNotificationCount();
        });
        sheet.show(getSupportFragmentManager(), "NotificationsBottomSheet");
    }

    public void showLoginDialog() {
        AuthDialog dialog = new AuthDialog(this, new AuthDialog.OnAuthSuccessListener() {
            @Override
            public void onLoginSuccess(String token, String fullName) {
                viewModel.setLoggedIn(true);
            }

            @Override
            public void onRegisterSuccess(String token, String fullName) {
                viewModel.setLoggedIn(true);
            }
        });
        dialog.show();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
