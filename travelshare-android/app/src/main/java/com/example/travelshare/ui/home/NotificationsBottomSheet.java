package com.example.travelshare.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelshare.R;
import com.example.travelshare.model.Notification;
import com.example.travelshare.network.TokenManager;
import com.example.travelshare.ui.detail.PhotoDetailActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class NotificationsBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView recyclerNotifications;
    private NotificationAdapter adapter;
    private TextView tvUnreadCount;
    private NotificationViewModel viewModel;
    private TokenManager tokenManager;
    private OnNotificationsReadListener listener;

    public interface OnNotificationsReadListener {
        void onNotificationsUpdated();
    }

    public NotificationsBottomSheet(OnNotificationsReadListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        tokenManager = new TokenManager(requireContext());

        recyclerNotifications = view.findViewById(R.id.recycler_notifications);
        tvUnreadCount = view.findViewById(R.id.tv_notif_unread_count);

        view.findViewById(R.id.btn_close_notifications).setOnClickListener(v -> dismiss());

        setupRecyclerView();
        setupObservers();

        if (tokenManager.isLoggedIn()) {
            viewModel.loadNotifications();
            viewModel.loadUnreadCount();
        }
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notification -> {
            if (tokenManager.isLoggedIn()) {
                viewModel.markAsRead(notification);
                if (listener != null) listener.onNotificationsUpdated();
            }
            
            handleNotificationClick(notification);
        });
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerNotifications.setAdapter(adapter);
    }

    private void handleNotificationClick(Notification notification) {
        String type = notification.getType();
        String relatedId = notification.getRelatedId();

        Log.d("NotifClick", "Type: " + type + ", ID: " + relatedId);

        if (relatedId == null || relatedId.isEmpty()) {
            Toast.makeText(requireContext(), "Données incomplètes (ID manquant)", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent;
        switch (type) {
            case "new_photo":
            case "new_like":
            case "new_comment":
            case "like":      // Au cas où le type MongoDB est juste "like"
            case "comment":   // Au cas où le type MongoDB est juste "comment"
                intent = new Intent(requireContext(), PhotoDetailActivity.class);
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, relatedId);
                startActivity(intent);
                dismiss();
                break;

            case "group_photo":
            case "group":
                intent = new Intent(requireContext(), GroupPhotosActivity.class);
                intent.putExtra("group_id", relatedId);
                intent.putExtra("group_name", "Groupe"); 
                startActivity(intent);
                dismiss();
                break;
                
            default:
                Toast.makeText(requireContext(), "Type inconnu : " + type, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setupObservers() {
        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications != null) {
                adapter.submitList(notifications);
            }
        });

        viewModel.getUnreadCount().observe(getViewLifecycleOwner(), unread -> {
            tvUnreadCount.setText((unread != null ? unread : 0) + " non lues");
        });
    }
}
