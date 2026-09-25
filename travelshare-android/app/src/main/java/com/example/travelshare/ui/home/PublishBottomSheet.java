package com.example.travelshare.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.travelshare.R;
import com.example.travelshare.model.Photo;
import com.example.travelshare.model.Group;
import com.example.travelshare.ui.detail.PhotoDetailActivity;
import com.example.travelshare.model.response.MessageResponse;
import com.example.travelshare.network.RetrofitClient;
import com.example.travelshare.network.TokenManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublishBottomSheet extends BottomSheetDialogFragment {

    private PublishViewModel viewModel;
    private PhotoViewModel photoViewModel;
    private GroupViewModel groupViewModel;
    private TokenManager tokenManager;
    private FusedLocationProviderClient fusedLocationClient;
    
    private ImageView   imgPreview;
    private View        layoutPlaceholder;
    private EditText    etDescription, etLocation, etCountry, etTagInput, etHowTo, etLatitude, etLongitude;
    private Spinner     spinnerLocationType;
    private ChipGroup   chipGroupTags;
    private RadioGroup  rgVisibility;
    private LinearLayout layoutGroups, containerGroups, layoutPublishLoading;
    private ImageButton btnPickLocation;
    private Button      btnSubmit;
    private TextView    tvPublishStatus;

    private final String[] LOCATION_TYPES = {"Nature", "Urbain", "Plage", "Montagne", "Musée", "Magasin", "Restaurant"};

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        viewModel.setSelectedImageUri(uri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    double lat = result.getData().getDoubleExtra("lat", 0.0);
                    double lng = result.getData().getDoubleExtra("lng", 0.0);
                    etLatitude.setText(String.format(Locale.US, "%.6f", lat));
                    etLongitude.setText(String.format(Locale.US, "%.6f", lng));
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_publish, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PublishViewModel.class);
        photoViewModel = new ViewModelProvider(requireActivity()).get(PhotoViewModel.class);
        groupViewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);
        
        tokenManager = new TokenManager(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        imgPreview = view.findViewById(R.id.img_preview);
        layoutPlaceholder = view.findViewById(R.id.layout_upload_placeholder);
        etDescription = view.findViewById(R.id.et_description);
        etLocation = view.findViewById(R.id.et_location);
        etCountry = view.findViewById(R.id.et_country);
        etTagInput = view.findViewById(R.id.et_tag_input);
        etHowTo = view.findViewById(R.id.et_how_to);
        etLatitude = view.findViewById(R.id.et_latitude);
        etLongitude = view.findViewById(R.id.et_longitude);
        spinnerLocationType = view.findViewById(R.id.spinner_location_type);
        chipGroupTags        = view.findViewById(R.id.chip_group_tags);
        rgVisibility         = view.findViewById(R.id.rg_visibility);
        layoutGroups         = view.findViewById(R.id.layout_groups);
        containerGroups      = view.findViewById(R.id.container_group_checkboxes);
        btnPickLocation      = view.findViewById(R.id.btn_pick_location);
        btnSubmit            = view.findViewById(R.id.btn_submit_publish);
        layoutPublishLoading = view.findViewById(R.id.layout_publish_loading);
        tvPublishStatus      = view.findViewById(R.id.tv_publish_status);

        setupSpinner();
        setupImagePicker(view);
        setupTagAddition(view);
        setupVisibilityLogic();
        setupObservers();

        btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });

        view.findViewById(R.id.btn_cancel_publish).setOnClickListener(v -> dismiss());
        btnSubmit.setOnClickListener(v -> uploadImageAndSubmit());
        
        groupViewModel.loadGroups();
        autoFillGpsLocation();
    }

    private void autoFillGpsLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null || etLatitude == null || etLongitude == null) return;
            if (etLatitude.getText().toString().isEmpty()) {
                etLatitude.setText(String.format(Locale.US, "%.6f", location.getLatitude()));
            }
            if (etLongitude.getText().toString().isEmpty()) {
                etLongitude.setText(String.format(Locale.US, "%.6f", location.getLongitude()));
            }
        });
    }

    private void setupObservers() {
        viewModel.getSelectedImageUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                imgPreview.setImageURI(uri);
                imgPreview.setVisibility(View.VISIBLE);
                layoutPlaceholder.setVisibility(View.GONE);
            }
        });

        viewModel.getTags().observe(getViewLifecycleOwner(), tagsList -> {
            chipGroupTags.removeAllViews();
            for (String tag : tagsList) {
                addTagToUI(tag);
            }
        });

        groupViewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            containerGroups.removeAllViews();
            if (groups != null) {
                for (Group group : groups) {
                    CheckBox cb = new CheckBox(requireContext());
                    cb.setText(group.getName());
                    cb.setTag(group.getId());
                    containerGroups.addView(cb);
                }
            }
        });
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, LOCATION_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocationType.setAdapter(adapter);
    }

    private void setupImagePicker(View view) {
        view.findViewById(R.id.btn_select_photo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }

    private void setupTagAddition(View view) {
        view.findViewById(R.id.btn_add_tag).setOnClickListener(v -> {
            String tag = etTagInput.getText().toString().trim();
            if (!tag.isEmpty()) {
                if (!tag.startsWith("#")) tag = "#" + tag;
                viewModel.addTag(tag);
                etTagInput.setText("");
            }
        });
    }

    private void addTagToUI(String tag) {
        Chip chip = new Chip(requireContext());
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.colorPrimary);
        chip.setTextColor(Color.WHITE);
        chip.setOnCloseIconClickListener(v -> viewModel.removeTag(tag));
        chipGroupTags.addView(chip);
    }

    private void setupVisibilityLogic() {
        rgVisibility.setOnCheckedChangeListener((group, checkedId) -> {
            layoutGroups.setVisibility(checkedId == R.id.rb_private ? View.VISIBLE : View.GONE);
        });
    }

    private void setLoading(boolean loading, String status) {
        btnSubmit.setEnabled(!loading);
        btnSubmit.setAlpha(loading ? 0.5f : 1f);
        layoutPublishLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (status != null) tvPublishStatus.setText(status);
    }

    private void uploadImageAndSubmit() {
        Uri uri = viewModel.getSelectedImageUri().getValue();
        if (uri == null) {
            Toast.makeText(getContext(), "Veuillez choisir une image", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = tokenManager.getBearerToken();
        if (token == null) {
            Toast.makeText(getContext(), "Vous devez être connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, "Préparation de l'image...");

        Activity activity = requireActivity();
        new Thread(() -> {
            File file = getCompressedImageFile(uri);
            activity.runOnUiThread(() -> {
                if (file == null) {
                    setLoading(false, null);
                    Toast.makeText(getContext(), "Impossible de lire le fichier", Toast.LENGTH_SHORT).show();
                    return;
                }

                RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
                MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

                setLoading(true, "Envoi de l'image...");

                RetrofitClient.getApiService().uploadPhoto(body)
                        .enqueue(new Callback<Map<String, String>>() {
                            @Override
                            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    String imageUrl = response.body().get("imageUrl");
                                    setLoading(true, "Publication en cours...");
                                    submitPhotoToBackend(imageUrl);
                                } else {
                                    Log.e("Upload", "Erreur: " + response.code());
                                    setLoading(false, null);
                                    Toast.makeText(getContext(), "Erreur serveur " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                                Log.e("Upload", "Echec: " + t.getMessage());
                                setLoading(false, null);
                                Toast.makeText(getContext(), "Erreur réseau upload", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }).start();
    }

    private File getCompressedImageFile(Uri uri) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream s = requireContext().getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(s, null, opts);
            s.close();

            int maxDim = 1080;
            int sample = 1;
            int w = opts.outWidth, h = opts.outHeight;
            while (Math.max(w, h) / (sample * 2) >= maxDim) sample *= 2;

            opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            InputStream stream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, opts);
            stream.close();

            if (bitmap == null) return null;

            File out = new File(requireContext().getCacheDir(), "upload_compressed.jpg");
            FileOutputStream fos = new FileOutputStream(out);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, fos);
            fos.flush();
            fos.close();
            bitmap.recycle();
            return out;
        } catch (Exception e) {
            Log.e("Upload", "Compression failed", e);
            return null;
        }
    }

    private void submitPhotoToBackend(String imageUrl) {
        Photo newPhoto = new Photo();
        newPhoto.setImageUrl(imageUrl);
        newPhoto.setDescription(etDescription.getText().toString().trim());
        newPhoto.setLocation(etLocation.getText().toString().trim());
        newPhoto.setCountry(etCountry.getText().toString().trim());
        
        try {
            newPhoto.setLatitude(Double.parseDouble(etLatitude.getText().toString()));
            newPhoto.setLongitude(Double.parseDouble(etLongitude.getText().toString()));
        } catch (Exception e) {
            newPhoto.setLatitude(0.0);
            newPhoto.setLongitude(0.0);
        }
        
        newPhoto.setLocationType(spinnerLocationType.getSelectedItem().toString());
        newPhoto.setTags(viewModel.getTags().getValue());
        newPhoto.setHowToGetThere(etHowTo.getText().toString());

        photoViewModel.publishPhoto(newPhoto, new PhotoViewModel.OnPublishListener() {
            @Override
            public void onSuccess(Photo publishedPhoto) {
                String photoId = publishedPhoto != null ? publishedPhoto.getId() : null;
                if (rgVisibility.getCheckedRadioButtonId() == R.id.rb_private) {
                    addPhotoToSelectedGroups(photoId);
                } else {
                    openDetailAfterPublish(photoId);
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false, null);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDetailAfterPublish(String photoId) {
        if (getContext() == null) return;
        if (photoId == null) {
            Toast.makeText(requireContext(), "Photo publiée !", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        Intent intent = new Intent(requireContext(), PhotoDetailActivity.class);
        intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photoId);
        intent.putExtra(PhotoDetailActivity.EXTRA_NEW_PHOTO, true);
        startActivity(intent);
        dismiss();
    }

    private int groupsProcessed = 0;
    private void addPhotoToSelectedGroups(String photoId) {
        if (photoId == null) {
            Toast.makeText(getContext(), "Photo publiée (ID manquant)", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        int totalSelected = 0;
        for (int i = 0; i < containerGroups.getChildCount(); i++) {
            View v = containerGroups.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox) v).isChecked()) {
                totalSelected++;
            }
        }

        if (totalSelected == 0) {
            Toast.makeText(getContext(), "Photo publiée !", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        groupsProcessed = 0;
        final int finalTotal = totalSelected;
        String token = tokenManager.getBearerToken();

        for (int i = 0; i < containerGroups.getChildCount(); i++) {
            View v = containerGroups.getChildAt(i);
            if (v instanceof CheckBox) {
                CheckBox cb = (CheckBox) v;
                if (cb.isChecked()) {
                    String groupId = (String) cb.getTag();
                    Map<String, Object> body = new HashMap<>();
                    body.put("photoId", photoId);

                    RetrofitClient.getApiService().addPhotoToGroup(groupId, body)
                            .enqueue(new Callback<MessageResponse>() {
                                @Override
                                public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                                    if (!response.isSuccessful()) {
                                        String err = "Erreur " + response.code();
                                        try { if(response.errorBody() != null) err += ": " + response.errorBody().string(); } catch(IOException e){}
                                        Log.e("Publish", err);
                                    }
                                    checkAllGroupsProcessed(finalTotal);
                                }

                                @Override
                                public void onFailure(Call<MessageResponse> call, Throwable t) {
                                    Log.e("Publish", "Echec ajout groupe", t);
                                    checkAllGroupsProcessed(finalTotal);
                                }
                            });
                }
            }
        }
    }

    private void checkAllGroupsProcessed(int total) {
        groupsProcessed++;
        if (groupsProcessed >= total) {
            Toast.makeText(getContext(), "Photo publiée et ajoutée aux groupes !", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }
}
