package com.example.travelshare.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelshare.R;
import com.example.travelshare.model.Group;
import com.example.travelshare.network.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class GroupsBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView recyclerGroups;
    private GroupAdapter adapter;
    private TextView tvCountBadge, tvTitle;
    private EditText etName, etDescription;
    private ImageView ivCoverPreview;
    private Button btnCreate, btnCancel, btnPickCover;
    private Button btnTabMyGroups, btnTabDiscover;
    private View layoutCreateGroup;
    private GroupViewModel viewModel;
    private TokenManager tokenManager;

    private Uri selectedImageUri;
    private boolean isDiscoverTab = false;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).centerCrop().into(ivCoverPreview);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);
        tokenManager = new TokenManager(requireContext());

        recyclerGroups = view.findViewById(R.id.recycler_groups);
        tvCountBadge = view.findViewById(R.id.tv_groups_count_badge);
        tvTitle = view.findViewById(R.id.tv_title_groups);
        etName = view.findViewById(R.id.et_group_name);
        etDescription = view.findViewById(R.id.et_group_description);
        ivCoverPreview = view.findViewById(R.id.iv_group_cover_preview);
        btnPickCover = view.findViewById(R.id.btn_pick_cover);
        btnCreate = view.findViewById(R.id.btn_create_group);
        btnCancel = view.findViewById(R.id.btn_cancel_create);
        btnTabMyGroups = view.findViewById(R.id.btn_tab_my_groups);
        btnTabDiscover = view.findViewById(R.id.btn_tab_discover);
        layoutCreateGroup = view.findViewById(R.id.layout_create_group);

        view.findViewById(R.id.btn_close_groups).setOnClickListener(v -> dismiss());

        setupRecyclerView();
        setupTabs();
        setupObservers();
        setupCreationLogic();

        viewModel.loadGroups();
        viewModel.loadDiscoverGroups();
    }

    private void setupRecyclerView() {
        adapter = new GroupAdapter(new GroupAdapter.OnGroupClickListener() {
            @Override
            public void onGroupClick(Group group) {
                Intent intent = new Intent(requireContext(), GroupPhotosActivity.class);
                intent.putExtra("group_id", group.getId());
                intent.putExtra("group_name", group.getName());
                intent.putExtra("group_desc", group.getDescription());
                intent.putExtra("group_cover", group.getCoverImageUrl());
                startActivity(intent);
            }

            @Override
            public void onJoinClick(Group group) {
                viewModel.joinGroup(group.getId(), new GroupViewModel.OnActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "Vous avez rejoint " + group.getName(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(), "Erreur : " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onLeaveClick(Group group) {
                confirmLeaveGroup(group);
            }

            @Override
            public void onGroupLongClick(Group group) {
                if (!isDiscoverTab) {
                    showGroupOptions(group);
                }
            }

            @Override
            public void onDeleteClick(Group group) {
                confirmDeleteGroup(group);
            }
        });
        
        adapter.setCurrentUserId(tokenManager.getUserId());
        recyclerGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerGroups.setAdapter(adapter);
    }

    private void showGroupOptions(Group group) {
        String currentUserId = tokenManager.getUserId();
        boolean isCreator = group.getCreator() != null && group.getCreator().getId().equals(currentUserId);

        String[] options;
        if (isCreator) {
            options = new String[]{"Supprimer le groupe", "Quitter le groupe"};
        } else {
            options = new String[]{"Quitter le groupe"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(group.getName())
                .setItems(options, (dialog, which) -> {
                    String selectedOption = options[which];
                    if (selectedOption.equals("Supprimer le groupe")) {
                        confirmDeleteGroup(group);
                    } else if (selectedOption.equals("Quitter le groupe")) {
                        confirmLeaveGroup(group);
                    }
                })
                .show();
    }

    private void confirmDeleteGroup(Group group) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Supprimer le groupe")
                .setMessage("Voulez-vous vraiment supprimer " + group.getName() + " ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    viewModel.deleteGroup(group.getId(), new GroupViewModel.OnActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(requireContext(), "Groupe supprimé", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), "Erreur: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void confirmLeaveGroup(Group group) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Quitter le groupe")
                .setMessage("Voulez-vous vraiment quitter " + group.getName() + " ?")
                .setPositiveButton("Quitter", (dialog, which) -> {
                    viewModel.leaveGroup(group.getId(), new GroupViewModel.OnActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(requireContext(), "Vous avez quitté le groupe", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), "Erreur: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void setupTabs() {
        btnTabMyGroups.setOnClickListener(v -> {
            isDiscoverTab = false;
            updateTabUI();
            viewModel.loadGroups();
        });

        btnTabDiscover.setOnClickListener(v -> {
            isDiscoverTab = true;
            updateTabUI();
            viewModel.loadDiscoverGroups();
        });
    }

    private void updateTabUI() {
        if (isDiscoverTab) {
            btnTabDiscover.setTextColor(Color.parseColor("#7C3AED"));
            btnTabMyGroups.setTextColor(Color.parseColor("#757575"));
            layoutCreateGroup.setVisibility(View.GONE);
            tvTitle.setText("Découvrir des groupes");
            adapter.setDiscoverMode(true);
            observeDiscoverGroups();
        } else {
            btnTabMyGroups.setTextColor(Color.parseColor("#7C3AED"));
            btnTabDiscover.setTextColor(Color.parseColor("#757575"));
            layoutCreateGroup.setVisibility(View.VISIBLE);
            tvTitle.setText("Mes Groupes");
            adapter.setDiscoverMode(false);
            observeMyGroups();
        }
    }

    private void setupObservers() {
        observeMyGroups();
    }

    private void observeMyGroups() {
        viewModel.getDiscoverGroups().removeObservers(getViewLifecycleOwner());
        viewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            if (!isDiscoverTab && groups != null) {
                adapter.submitList(groups);
                tvCountBadge.setText(String.valueOf(groups.size()));
            }
        });
    }

    private void observeDiscoverGroups() {
        viewModel.getGroups().removeObservers(getViewLifecycleOwner());
        viewModel.getDiscoverGroups().observe(getViewLifecycleOwner(), groups -> {
            if (isDiscoverTab && groups != null) {
                adapter.submitList(groups);
                tvCountBadge.setText(String.valueOf(groups.size()));
            }
        });
    }

    private void setupCreationLogic() {
        btnPickCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String cover = selectedImageUri != null ? selectedImageUri.toString() : "";

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Le nom du groupe est obligatoire", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.createNewGroup(name, desc, cover);
            
            resetForm();
            Toast.makeText(requireContext(), "Groupe créé !", Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(v -> resetForm());
    }

    private void resetForm() {
        etName.setText("");
        etDescription.setText("");
        selectedImageUri = null;
        ivCoverPreview.setImageResource(R.drawable.placeholder_photo);
    }
}
