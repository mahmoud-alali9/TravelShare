package com.example.travelshare.ui.auth;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.travelshare.R;
import com.example.travelshare.model.response.AuthResponse;
import com.example.travelshare.network.RetrofitClient;
import com.example.travelshare.network.TokenManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthDialog extends Dialog {

    // Interface pour notifier l'activité quand la connexion réussit
    public interface OnAuthSuccessListener {
        void onLoginSuccess(String token, String fullName);
        void onRegisterSuccess(String token, String fullName);
    }

    private OnAuthSuccessListener listener;

    // Onglets
    private TextView     tabLogin, tabRegister;
    private LinearLayout formLogin, formRegister;

    // Champs connexion
    private EditText etLoginEmail, etLoginPassword;
    private Button   btnLoginSubmit;

    // Champs inscription
    private EditText etRegFullname, etRegUsername, etRegEmail, etRegPassword;
    private Button   btnRegisterSubmit;

    public AuthDialog(@NonNull Context context, OnAuthSuccessListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_auth);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        bindViews();
        setupTabs();
        setupButtons();
    }

    private void bindViews() {
        // Onglets
        tabLogin     = findViewById(R.id.tab_login);
        tabRegister  = findViewById(R.id.tab_register);
        formLogin    = findViewById(R.id.form_login);
        formRegister = findViewById(R.id.form_register);

        // Champs connexion
        etLoginEmail    = findViewById(R.id.et_login_email);
        etLoginPassword = findViewById(R.id.et_login_password);
        btnLoginSubmit  = findViewById(R.id.btn_login_submit);

        // Champs inscription
        etRegFullname     = findViewById(R.id.et_reg_fullname);
        etRegUsername     = findViewById(R.id.et_reg_username);
        etRegEmail        = findViewById(R.id.et_reg_email);
        etRegPassword     = findViewById(R.id.et_reg_password);
        btnRegisterSubmit = findViewById(R.id.btn_register_submit);
    }

    private void setupTabs() {
        tabLogin.setOnClickListener(v -> showLoginForm());
        tabRegister.setOnClickListener(v -> showRegisterForm());
    }

    private void showLoginForm() {
        formLogin.setVisibility(View.VISIBLE);
        formRegister.setVisibility(View.GONE);
        tabLogin.setBackgroundResource(R.drawable.bg_tab_selected);
        tabLogin.setTextColor(getContext().getColor(android.R.color.white));
        tabRegister.setBackgroundResource(R.drawable.bg_tab_normal);
        tabRegister.setTextColor(0xFF9E9E9E);
    }

    private void showRegisterForm() {
        formLogin.setVisibility(View.GONE);
        formRegister.setVisibility(View.VISIBLE);
        tabRegister.setBackgroundResource(R.drawable.bg_tab_selected);
        tabRegister.setTextColor(getContext().getColor(android.R.color.white));
        tabLogin.setBackgroundResource(R.drawable.bg_tab_normal);
        tabLogin.setTextColor(0xFF9E9E9E);
    }

    private void setupButtons() {

        // ── Bouton connexion ──────────────────────────────
        btnLoginSubmit.setOnClickListener(v -> {
            String email    = etLoginEmail.getText().toString().trim();
            String password = etLoginPassword.getText().toString().trim();

            // Vérification des champs obligatoires
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(),
                        "Veuillez remplir tous les champs",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Appel API connexion
            loginWithApi(email, password);
        });

        // ── Bouton inscription ────────────────────────────
        btnRegisterSubmit.setOnClickListener(v -> {
            String fullName = etRegFullname.getText().toString().trim();
            String username = etRegUsername.getText().toString().trim();
            String email    = etRegEmail.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();

            // Vérification des champs obligatoires
            if (fullName.isEmpty() || username.isEmpty()
                    || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(),
                        "Veuillez remplir tous les champs",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérification longueur mot de passe
            if (password.length() < 6) {
                Toast.makeText(getContext(),
                        "Le mot de passe doit contenir au moins 6 caractères",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Appel API inscription
            registerWithApi(fullName, username, email, password);
        });
    }

    // ── Appel API Connexion ───────────────────────────────
    private void loginWithApi(String email, String password) {
        // Désactive le bouton pendant la requête
        btnLoginSubmit.setEnabled(false);
        btnLoginSubmit.setText("Connexion...");

        // Prépare les données à envoyer au serveur
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        // Envoie la requête au backend
        RetrofitClient.getApiService().login(body).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(Call<AuthResponse> call,
                                   Response<AuthResponse> response) {
                // Réactive le bouton
                btnLoginSubmit.setEnabled(true);
                btnLoginSubmit.setText("Se connecter");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();

                    // Sauvegarde le token et les infos utilisateur localement
                    TokenManager tokenManager = new TokenManager(getContext());
                    tokenManager.saveToken(auth.getToken());
                    tokenManager.saveUser(
                            auth.getUser().getFullName(),
                            auth.getUser().getUsername(),
                            auth.getUser().getId()
                    );
                    tokenManager.saveCreatedAt(auth.getUser().getCreatedAt());

                    Toast.makeText(getContext(),
                            "Bienvenue " + auth.getUser().getFullName(),
                            Toast.LENGTH_SHORT).show();

                    // Notifie le fragment que la connexion a réussi
                    if (listener != null) {
                        listener.onLoginSuccess(
                                auth.getToken(),
                                auth.getUser().getFullName()
                        );
                    }
                    dismiss();
                } else {
                    // Le serveur a répondu mais avec une erreur
                    Toast.makeText(getContext(),
                            "Email ou mot de passe incorrect",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Erreur réseau (pas de connexion au serveur)
                btnLoginSubmit.setEnabled(true);
                btnLoginSubmit.setText("Se connecter");
                Toast.makeText(getContext(),
                        "Erreur de connexion au serveur",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Appel API Inscription ─────────────────────────────
    private void registerWithApi(String fullName, String username,
                                 String email, String password) {
        // Désactive le bouton pendant la requête
        btnRegisterSubmit.setEnabled(false);
        btnRegisterSubmit.setText("Inscription...");

        // Prépare les données à envoyer au serveur
        Map<String, String> body = new HashMap<>();
        body.put("fullName", fullName);
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);

        // Envoie la requête au backend
        RetrofitClient.getApiService().register(body).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(Call<AuthResponse> call,
                                   Response<AuthResponse> response) {
                // Réactive le bouton
                btnRegisterSubmit.setEnabled(true);
                btnRegisterSubmit.setText("S'inscrire");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();

                    // Sauvegarde le token et les infos utilisateur localement
                    TokenManager tokenManager = new TokenManager(getContext());
                    tokenManager.saveToken(auth.getToken());
                    tokenManager.saveUser(
                            auth.getUser().getFullName(),
                            auth.getUser().getUsername(),
                            auth.getUser().getId()
                    );
                    tokenManager.saveCreatedAt(auth.getUser().getCreatedAt());

                    Toast.makeText(getContext(),
                            "Compte créé avec succès !",
                            Toast.LENGTH_SHORT).show();

                    // Notifie le fragment que l'inscription a réussi
                    if (listener != null) {
                        listener.onRegisterSuccess(
                                auth.getToken(),
                                auth.getUser().getFullName()
                        );
                    }
                    dismiss();
                } else {
                    // Le serveur a répondu mais avec une erreur
                    Toast.makeText(getContext(),
                            "Erreur lors de l'inscription",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Erreur réseau
                btnRegisterSubmit.setEnabled(true);
                btnRegisterSubmit.setText("S'inscrire");
                Toast.makeText(getContext(),
                        "Erreur de connexion au serveur",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
