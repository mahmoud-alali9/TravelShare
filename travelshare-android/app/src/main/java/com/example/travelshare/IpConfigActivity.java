package com.example.travelshare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelshare.network.RetrofitClient;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IpConfigActivity extends AppCompatActivity {

    private EditText etIpAddress;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_config);

        etIpAddress = findViewById(R.id.et_ip_address);
        btnConnect  = findViewById(R.id.btn_connect);

        // Pre-fill with the correct target server IP
        etIpAddress.setText("172.20.10.2");

        // Load saved IP if exists
        SharedPreferences prefs = getSharedPreferences("TravelShare", MODE_PRIVATE);
        String savedIp = prefs.getString("server_ip", "");
        
        // FORCE RESET if the saved IP is the old problematic one
        if ("10.42.27.244".equals(savedIp) || "172.16.3.136".equals(savedIp)) {
            savedIp = "";
            prefs.edit().remove("server_ip").apply();
        }

        // If IP already saved (and not the old ones), skip config screen
        if (!savedIp.isEmpty()) {
            RetrofitClient.setBaseUrl("http://" + savedIp + ":3000/");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        btnConnect.setOnClickListener(v -> {
            String ip = etIpAddress.getText().toString().trim();

            if (ip.isEmpty()) {
                Toast.makeText(this, "Please enter IP address", Toast.LENGTH_SHORT).show();
                return;
            }

            // Test connection before saving
            testConnection(ip);
        });
    }

    private void testConnection(String ip) {
        btnConnect.setEnabled(false);
        btnConnect.setText("Connecting...");

        // Test in background thread
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url("http://" + ip + ":3000/api/photos")
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    // Save IP and go to main activity
                    runOnUiThread(() -> {
                        saveIpAndContinue(ip);
                    });
                } else {
                    runOnUiThread(() -> {
                        btnConnect.setEnabled(true);
                        btnConnect.setText("Connect");
                        Toast.makeText(this,
                                "Server responded with error: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Connect");
                    Toast.makeText(this,
                            "Cannot reach server at " + ip,
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveIpAndContinue(String ip) {
        // Save IP
        getSharedPreferences("TravelShare", MODE_PRIVATE)
                .edit()
                .putString("server_ip", ip)
                .apply();

        // Update RetrofitClient with new IP
        RetrofitClient.setBaseUrl("http://" + ip + ":3000/");

        // Go to MainActivity
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
