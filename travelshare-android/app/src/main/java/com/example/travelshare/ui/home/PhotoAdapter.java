package com.example.travelshare.ui.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelshare.R;
import com.example.travelshare.model.Photo;
import com.example.travelshare.ui.detail.PhotoDetailActivity;

public class PhotoAdapter extends ListAdapter<Photo, RecyclerView.ViewHolder> {

    public static final int VIEW_GRID = 0;
    public static final int VIEW_LIST = 1;
    public static final int TYPE_BANNER = 2;

    private final Context context;
    private int viewType = VIEW_GRID;
    private boolean isUserLoggedIn = false;
    private String currentUserId;
    private OnConnectClickListener connectClickListener;
    private OnPhotoDeleteListener deleteListener;

    public interface OnConnectClickListener {
        void onConnectClick();
    }

    public interface OnPhotoDeleteListener {
        void onPhotoDelete(Photo photo);
    }

    private static final DiffUtil.ItemCallback<Photo> DIFF_CALLBACK = new DiffUtil.ItemCallback<Photo>() {
        @Override
        public boolean areItemsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
            return oldItem.getLikeCount() == newItem.getLikeCount() &&
                    oldItem.isLikedByMe() == newItem.isLikedByMe() &&
                    oldItem.getImageUrl().equals(newItem.getImageUrl()) &&
                    oldItem.getDescription().equals(newItem.getDescription());
        }
    };

    public PhotoAdapter(Context context, int viewType) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.viewType = viewType;
    }

    public void setViewMode(int viewType) {
        this.viewType = viewType;
        notifyDataSetChanged();
    }

    public void setLoggedIn(boolean loggedIn) {
        this.isUserLoggedIn = loggedIn;
        notifyDataSetChanged();
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        notifyDataSetChanged();
    }

    public void setOnConnectClickListener(OnConnectClickListener listener) {
        this.connectClickListener = listener;
    }

    public void setOnPhotoDeleteListener(OnPhotoDeleteListener listener) {
        this.deleteListener = listener;
    }

    public boolean shouldShowBanner() {
        return !isUserLoggedIn;
    }

    @Override
    public int getItemViewType(int position) {
        if (shouldShowBanner() && position == 0) return TYPE_BANNER;
        return viewType;
    }

    @Override
    public int getItemCount() {
        int count = getCurrentList().size();
        return shouldShowBanner() ? count + 1 : count;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (type == TYPE_BANNER) {
            return new BannerViewHolder(inflater.inflate(R.layout.item_banner_anonymous, parent, false));
        } else if (type == VIEW_GRID) {
            return new GridViewHolder(inflater.inflate(R.layout.item_photo_grid, parent, false));
        } else {
            return new PhotoViewHolder(inflater.inflate(R.layout.item_photo_list, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BannerViewHolder) {
            ((BannerViewHolder) holder).btnConnect.setOnClickListener(v -> {
                if (connectClickListener != null) {
                    connectClickListener.onConnectClick();
                }
            });
        } else {
            int actualPos = shouldShowBanner() ? position - 1 : position;
            if (actualPos < 0 || actualPos >= getCurrentList().size()) return;
            
            Photo photo = getItem(actualPos);
            
            if (holder instanceof GridViewHolder) bindGrid((GridViewHolder) holder, photo);
            else if (holder instanceof PhotoViewHolder) bindList((PhotoViewHolder) holder, photo);
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, PhotoDetailActivity.class);
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photo.getId());
                context.startActivity(intent);
            });
        }
    }

    private void bindGrid(GridViewHolder h, Photo photo) {
        Glide.with(context)
                .load(photo.getImageUrl())
                .centerCrop()
                .placeholder(R.drawable.placeholder_photo)
                .error(R.drawable.placeholder_photo)
                .into(h.imgPhoto);
        h.tvLocation.setText(photo.getLocation());
        h.tvLikeCount.setText(String.valueOf(photo.getLikeCount()));

        // Afficher la poubelle si c'est ma photo
        boolean isMine = currentUserId != null && photo.getAuthor() != null && currentUserId.equals(photo.getAuthor().getId());
        h.btnDelete.setVisibility(isMine ? View.VISIBLE : View.GONE);
        h.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onPhotoDelete(photo);
        });
    }

    private void bindList(PhotoViewHolder h, Photo photo) {
        Glide.with(context)
                .load(photo.getImageUrl())
                .centerCrop()
                .placeholder(R.drawable.placeholder_photo)
                .error(R.drawable.placeholder_photo)
                .into(h.imgPhoto);
        
        h.tvAuthorName.setText(photo.getAuthorName());
        h.tvLocation.setText(photo.getLocation() + (photo.getCountry() != null ? ", " + photo.getCountry() : ""));
        h.tvDescription.setText(photo.getDescription());
        h.tvLikeCount.setText(String.valueOf(photo.getLikeCount()));
        
        if (h.tvAvatar != null && photo.getAuthorName() != null && !photo.getAuthorName().isEmpty()) {
            h.tvAvatar.setText(photo.getAuthorName().substring(0, 1).toUpperCase());
        }

        // Afficher la poubelle si c'est ma photo
        boolean isMine = currentUserId != null && photo.getAuthor() != null && currentUserId.equals(photo.getAuthor().getId());
        h.btnDelete.setVisibility(isMine ? View.VISIBLE : View.GONE);
        h.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onPhotoDelete(photo);
        });
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        Button btnConnect;
        BannerViewHolder(View v) { super(v); btnConnect = v.findViewById(R.id.btn_connect); }
    }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        TextView tvLocation, tvLikeCount;
        ImageButton btnDelete;
        GridViewHolder(View v) { 
            super(v); 
            imgPhoto = v.findViewById(R.id.img_photo); 
            tvLocation = v.findViewById(R.id.tv_location);
            tvLikeCount = v.findViewById(R.id.tv_like_count);
            btnDelete = v.findViewById(R.id.btn_delete_photo);
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        TextView tvAuthorName, tvLocation, tvDescription, tvLikeCount, tvAvatar;
        ImageButton btnDelete;
        PhotoViewHolder(View v) { 
            super(v); 
            imgPhoto = v.findViewById(R.id.img_photo);
            tvAuthorName = v.findViewById(R.id.tv_author_name);
            tvLocation = v.findViewById(R.id.tv_location);
            tvDescription = v.findViewById(R.id.tv_description);
            tvLikeCount = v.findViewById(R.id.tv_like_count);
            tvAvatar = v.findViewById(R.id.tv_avatar);
            btnDelete = v.findViewById(R.id.btn_delete_photo);
        }
    }
}
