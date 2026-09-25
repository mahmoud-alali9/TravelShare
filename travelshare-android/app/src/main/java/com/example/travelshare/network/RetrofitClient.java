package com.example.travelshare.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static String BASE_URL = "http://172.20.10.2:3000/";
    private static Retrofit retrofit = null;
    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static void setBaseUrl(String url) {
        BASE_URL = url;
        retrofit = null; 
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            // Si le process a redémarré (crash) sans passer par IpConfigActivity,
            // restaurer l'IP depuis SharedPreferences
            if (appContext != null) {
                SharedPreferences prefs = appContext.getSharedPreferences("TravelShare", Context.MODE_PRIVATE);
                String savedIp = prefs.getString("server_ip", "");
                if (!savedIp.isEmpty()) {
                    BASE_URL = "http://" + savedIp + ":3000/";
                }
            }

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS);

            // Ajout automatique du Token s'il existe (avant le logging)
            if (appContext != null) {
                clientBuilder.addInterceptor(chain -> {
                    TokenManager tokenManager = new TokenManager(appContext);
                    String token = tokenManager.getToken();
                    Request.Builder newRequest = chain.request().newBuilder();
                    if (token != null) {
                        newRequest.header("Authorization", "Bearer " + token);
                        Log.d("AUTH_INTERCEPTOR", "Token envoyé: Bearer " + token.substring(0, Math.min(20, token.length())) + "...");
                    } else {
                        Log.w("AUTH_INTERCEPTOR", "Aucun token trouvé — requête envoyée sans Authorization");
                    }
                    return chain.proceed(newRequest.build());
                });
            } else {
                Log.e("AUTH_INTERCEPTOR", "CRITIQUE: appContext est null — intercepteur auth non ajouté !");
            }

            clientBuilder.addInterceptor(interceptor);

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(clientBuilder.build())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
