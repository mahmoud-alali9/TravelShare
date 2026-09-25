package com.example.travelshare.ui.home;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelshare.R;
import com.example.travelshare.model.Notification;

public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(OnNotificationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK = new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.isRead() == newItem.isRead() &&
                    oldItem.getMessage().equals(newItem.getMessage()) &&
                    oldItem.getTitle().equals(newItem.getTitle());
        }
    };

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = getItem(position);
        holder.bind(notification, listener);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvDate;
        View viewUnread;

        NotificationViewHolder(View v) {
            super(v);
            ivIcon = v.findViewById(R.id.iv_notification_icon);
            tvTitle = v.findViewById(R.id.tv_notification_title);
            tvMessage = v.findViewById(R.id.tv_notification_message);
            tvDate = v.findViewById(R.id.tv_notification_date);
            viewUnread = v.findViewById(R.id.view_unread_indicator);
        }

        void bind(final Notification notification, final OnNotificationClickListener listener) {
            tvTitle.setText(notification.getTitle());
            tvMessage.setText(notification.getMessage());
            tvDate.setText(notification.getDate());

            // Background color based on read status
            if (notification.isRead()) {
                itemView.setBackgroundColor(Color.WHITE);
                viewUnread.setVisibility(View.GONE);
            } else {
                itemView.setBackgroundColor(Color.parseColor("#F0F4FF"));
                viewUnread.setVisibility(View.VISIBLE);
            }

            // Icon based on type
            switch (notification.getType()) {
                case "new_photo":
                    ivIcon.setImageResource(R.drawable.ic_share);
                    ivIcon.setColorFilter(Color.parseColor("#2196F3"));
                    ivIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F0F4FF")));
                    break;
                case "new_like":
                    ivIcon.setImageResource(R.drawable.ic_favorite);
                    ivIcon.setColorFilter(Color.parseColor("#F44336"));
                    ivIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF0F0")));
                    break;
                case "group_photo":
                    ivIcon.setImageResource(R.drawable.ic_person);
                    ivIcon.setColorFilter(Color.parseColor("#7C3AED"));
                    ivIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F3F0FF")));
                    break;
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(notification);
            });
        }
    }
}
