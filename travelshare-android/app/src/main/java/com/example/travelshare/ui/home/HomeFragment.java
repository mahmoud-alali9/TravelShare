package com.example.travelshare.ui.home;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.travelshare.MainActivity;
import com.example.travelshare.MainViewModel;
import com.example.travelshare.R;
import com.example.travelshare.model.Comment;
import com.example.travelshare.model.Photo;
import com.example.travelshare.network.RetrofitClient;
import com.example.travelshare.network.TokenManager;
import com.example.travelshare.ui.detail.PhotoDetailActivity;
import com.example.travelshare.ui.detail.PhotoDetailViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private PhotoViewModel viewModel;
    private MainViewModel mainViewModel;
    private PhotoDetailViewModel detailViewModel;
    private PhotoAdapter adapter;

    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private RecyclerView recycler;
    private TextView tvPhotoCount;
    private EditText etSearch;
    private ImageButton btnMic, btnGrid, btnList, btnMap;
    private Spinner spinnerType;
    private FloatingActionButton fabPublish, fabGroups;
    private LinearLayout layoutEmptyState;
    private View mapContainerWrapper;
    private GoogleMap mMap;
    private TokenManager tokenManager;
    private SwipeRefreshLayout swipeRefresh;
    
    private FiltersDialog currentFiltersDialog;

    private static final String[] TYPES = {"Tous les types", "Plage", "Nature", "Urbain", "Montagne", "Musée"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(PhotoViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        detailViewModel = new ViewModelProvider(requireActivity()).get(PhotoDetailViewModel.class);
        tokenManager = new TokenManager(requireContext());

        bindViews(view);
        setupRecyclerView();
        setupSearchLogic();
        setupObservers();
        setupViewSwitchers();
        setupTypeSpinner();
        setupFABs(view);

        swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadPhotos();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        return view;
    }

    private void bindViews(View view) {
        recycler = view.findViewById(R.id.recycler_photos);
        tvPhotoCount = view.findViewById(R.id.tv_photo_count);
        etSearch = view.findViewById(R.id.et_search);
        btnMic = view.findViewById(R.id.btn_mic);
        btnGrid = view.findViewById(R.id.btn_grid);
        btnList = view.findViewById(R.id.btn_list);
        btnMap = view.findViewById(R.id.btn_map);
        spinnerType = view.findViewById(R.id.spinner_type);
        fabPublish = view.findViewById(R.id.fab_publish);
        fabGroups = view.findViewById(R.id.fab_groups);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        mapContainerWrapper = view.findViewById(R.id.map_container_wrapper);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
    }

    private void setupRecyclerView() {
        adapter = new PhotoAdapter(requireContext(), PhotoAdapter.VIEW_GRID);
        adapter.setOnConnectClickListener(() -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showLoginDialog();
            }
        });

        adapter.setOnPhotoDeleteListener(photo -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Supprimer la photo")
                    .setMessage("Voulez-vous vraiment supprimer cette photo ?")
                    .setPositiveButton("Supprimer", (dialog, which) -> {
                        if (tokenManager.isLoggedIn()) {
                            viewModel.deletePhoto(photo.getId(), new PhotoViewModel.OnDeleteListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Photo supprimée", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });

        setGridMode();
    }

    private void setupSearchLogic() {
        btnMic.setOnClickListener(v -> checkPermissionAndStartSpeech());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.setSearchQuery(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                String text = s.toString();
                searchRunnable = () -> viewModel.setSearchQuery(text);
                searchHandler.postDelayed(searchRunnable, 400);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkPermissionAndStartSpeech() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            startVoiceRecognition();
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez pour rechercher des lieux...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
            btnMic.setColorFilter(Color.RED); 
        } catch (Exception e) {
            Toast.makeText(getContext(), "Reconnaissance vocale non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        btnMic.setColorFilter(Color.parseColor("#757575")); 

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                etSearch.setText(spokenText);
                viewModel.setSearchQuery(spokenText);
            }
        } else if (requestCode == FiltersDialog.REQUEST_CODE_PICK_MAP && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("lat", 0);
            double lng = data.getDoubleExtra("lng", 0);
            if (currentFiltersDialog != null && currentFiltersDialog.isShowing()) {
                currentFiltersDialog.updateLocation(lat, lng);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                Toast.makeText(getContext(), "Permission microphone refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupObservers() {
        viewModel.getPhotos().observe(getViewLifecycleOwner(), photos -> {
            swipeRefresh.setRefreshing(false);
            if (photos != null) {
                adapter.submitList(new ArrayList<>(photos));
                updateCount(photos.size());
                
                if (layoutEmptyState != null) {
                    if (mapContainerWrapper.getVisibility() == View.VISIBLE) {
                        layoutEmptyState.setVisibility(View.GONE);
                    } else {
                        layoutEmptyState.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
                        recycler.setVisibility(photos.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }

                if (mapContainerWrapper != null && mapContainerWrapper.getVisibility() == View.VISIBLE && mMap != null) {
                    addMarkers(photos);
                }
            }
        });

        mainViewModel.isUserLoggedIn().observe(getViewLifecycleOwner(), loggedIn -> {
            int visibility = loggedIn ? View.VISIBLE : View.GONE;
            fabPublish.setVisibility(visibility);
            fabGroups.setVisibility(visibility);
            
            if (adapter != null) {
                adapter.setLoggedIn(loggedIn);
                if (loggedIn) {
                    adapter.setCurrentUserId(tokenManager.getUserId());
                }
            }
        });
    }

    private void setupViewSwitchers() {
        btnGrid.setOnClickListener(v -> {
            setSelected(btnGrid, btnList, btnMap);
            setGridMode();
        });
        btnList.setOnClickListener(v -> {
            setSelected(btnList, btnGrid, btnMap);
            setListMode();
        });
        btnMap.setOnClickListener(v -> {
            setSelected(btnMap, btnGrid, btnList);
            setMapMode();
        });
    }

    private void setupTypeSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, TYPES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                viewModel.filterByType(TYPES[pos]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupFABs(View view) {
        view.findViewById(R.id.btn_filters).setOnClickListener(v -> {
            currentFiltersDialog = new FiltersDialog(requireContext(), this, viewModel.getPhotos().getValue(), criteria -> viewModel.updateFilterCriteria(criteria));
            currentFiltersDialog.show();
        });

        fabPublish.setOnClickListener(v -> {
            PublishBottomSheet publishSheet = new PublishBottomSheet();
            publishSheet.show(getChildFragmentManager(), "PublishBottomSheet");
        });

        fabGroups.setOnClickListener(v -> {
            GroupsBottomSheet groupsSheet = new GroupsBottomSheet();
            groupsSheet.show(getChildFragmentManager(), "GroupsBottomSheet");
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        List<Photo> photos = viewModel.getPhotos().getValue();
        if (photos != null) addMarkers(photos);
        mMap.setOnInfoWindowClickListener(marker -> {
            Photo photo = (Photo) marker.getTag();
            if (photo != null) showPhotoBottomSheet(photo);
        });
    }

    private void addMarkers(List<Photo> photos) {
        if (mMap == null || photos == null) return;
        mMap.clear();
        for (Photo photo : photos) {
            LatLng pos = new LatLng(photo.getLatitude(), photo.getLongitude());
            Marker marker = mMap.addMarker(new MarkerOptions().position(pos).title(photo.getLocation()).snippet("Par " + photo.getAuthorName()));
            if (marker != null) marker.setTag(photo);
        }
    }

    private void setSelected(ImageButton selected, ImageButton... others) {
        selected.setBackgroundResource(R.drawable.bg_view_btn_selected);
        selected.setColorFilter(Color.WHITE);
        for (ImageButton other : others) {
            other.setBackgroundResource(R.drawable.bg_view_btn_normal);
            other.setColorFilter(Color.parseColor("#757575"));
        }
    }

    private void setGridMode() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getItemViewType(position) == PhotoAdapter.TYPE_BANNER ? 2 : 1;
            }
        });
        recycler.setLayoutManager(layoutManager);
        adapter.setViewMode(PhotoAdapter.VIEW_GRID);
        recycler.setAdapter(adapter);
        mapContainerWrapper.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void setListMode() {
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter.setViewMode(PhotoAdapter.VIEW_LIST);
        recycler.setAdapter(adapter);
        mapContainerWrapper.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void setListModeMode() {
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter.setViewMode(PhotoAdapter.VIEW_LIST);
        recycler.setAdapter(adapter);
        mapContainerWrapper.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void setMapMode() {
        recycler.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        mapContainerWrapper.setVisibility(View.VISIBLE);
        if (mMap != null) {
            List<Photo> photos = viewModel.getPhotos().getValue();
            if (photos != null) addMarkers(photos);
        }
    }

    private void updateCount(int count) {
        tvPhotoCount.setText(count + " photos partagées");
    }

    private void showPhotoBottomSheet(Photo photo) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_photo_bottom_sheet, null);
        
        ImageView ivPhoto = view.findViewById(R.id.bs_img_photo);
        TextView tvLoc = view.findViewById(R.id.bs_tv_location);
        TextView tvAuthor = view.findViewById(R.id.bs_tv_author);
        TextView tvDesc = view.findViewById(R.id.bs_tv_description);
        TextView tvCount = view.findViewById(R.id.tv_comments_count);
        RecyclerView rvComments = view.findViewById(R.id.rv_comments);
        EditText etComment = view.findViewById(R.id.et_comment);
        ImageButton btnSend = view.findViewById(R.id.btn_send_comment);
        LinearLayout layoutAddComment = view.findViewById(R.id.layout_add_comment);
        TextView tvLoginMsg = view.findViewById(R.id.tv_login_to_comment);
        
        Glide.with(this).load(photo.getImageUrl()).into(ivPhoto);
        tvLoc.setText(photo.getLocation());
        tvAuthor.setText("Par " + photo.getAuthorName());
        tvDesc.setText(photo.getDescription());

        CommentAdapter commentAdapter = new CommentAdapter();
        commentAdapter.setCurrentUsername(tokenManager.getUsername());
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(commentAdapter);

        boolean isLoggedIn = tokenManager.isLoggedIn();
        layoutAddComment.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        tvLoginMsg.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);

        detailViewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
            if (comments != null) {
                commentAdapter.setComments(new ArrayList<>(comments));
                tvCount.setText(comments.size() + " commentaires");
            }
        });
        detailViewModel.loadComments(photo.getId());

        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (text.isEmpty()) return;

            detailViewModel.postComment(photo.getId(), text, new PhotoDetailViewModel.OnCommentAddedListener() {
                @Override
                public void onSuccess() {
                    etComment.setText("");
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        commentAdapter.setDeleteListener(comment -> {
            detailViewModel.deleteComment(photo.getId(), comment.getId(), new PhotoDetailViewModel.OnCommentDeletedListener() {
                @Override
                public void onSuccess() {}

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        
        view.findViewById(R.id.bs_btn_details).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(getActivity(), PhotoDetailActivity.class);
            intent.putExtra("photo_id", photo.getId());
            startActivity(intent);
        });
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
}
