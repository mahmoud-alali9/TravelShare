package com.example.travelshare.ui.home;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelshare.R;
import com.example.travelshare.model.Group;

import java.util.List;

public class GroupAdapter extends ListAdapter<Group, GroupAdapter.GroupViewHolder> {

    private final OnGroupClickListener listener;
    private boolean isDiscoverMode = false;
    private String currentUserId;

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
        default void onJoinClick(Group group) {}
        default void onLeaveClick(Group group) {}
        default void onGroupLongClick(Group group) {}
        default void onDeleteClick(Group group) {}
    }

    public GroupAdapter(OnGroupClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setDiscoverMode(boolean discoverMode) {
        this.isDiscoverMode = discoverMode;
        notifyDataSetChanged();
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<Group> DIFF_CALLBACK = new DiffUtil.ItemCallback<Group>() {
        @Override
        public boolean areItemsTheSame(@NonNull Group oldItem, @NonNull Group newItem) {
            if (oldItem.getId() == null || newItem.getId() == null) return false;
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Group oldItem, @NonNull Group newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getDescription().equals(newItem.getDescription()) &&
                    oldItem.getMemberCount() == newItem.getMemberCount();
        }
    };

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(getItem(position), listener, isDiscoverMode, currentUserId);
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName, tvDescription, tvMemberCountBadge, tvMemberCountText;
        FrameLayout flAvatarStack;
        Button btnJoin;
        ImageButton btnDelete;

        GroupViewHolder(View v) {
            super(v);
            ivCover = v.findViewById(R.id.iv_group_cover);
            tvName = v.findViewById(R.id.tv_group_name);
            tvDescription = v.findViewById(R.id.tv_group_description);
            tvMemberCountBadge = v.findViewById(R.id.tv_member_count_badge);
            tvMemberCountText = v.findViewById(R.id.tv_member_count_text);
            flAvatarStack = v.findViewById(R.id.fl_avatar_stack);
            btnJoin = v.findViewById(R.id.btn_join_group);
            btnDelete = v.findViewById(R.id.btn_delete_group);
        }

        void bind(final Group group, final OnGroupClickListener listener, boolean isDiscoverMode, String currentUserId) {
            tvName.setText(group.getName());
            tvDescription.setText(group.getDescription());
            tvMemberCountBadge.setText(String.valueOf(group.getMemberCount()));
            tvMemberCountText.setText(group.getMemberCount() + " membres");

            Glide.with(itemView.getContext())
                    .load(group.getCoverImageUrl())
                    .placeholder(R.drawable.placeholder_photo)
                    .centerCrop()
                    .into(ivCover);

            if (flAvatarStack != null) {
                flAvatarStack.setVisibility(isDiscoverMode ? View.GONE : View.VISIBLE);
                flAvatarStack.removeAllViews();
                List<Group.Member> members = group.getMembers();
                if (members != null && !isDiscoverMode) {
                    int maxAvatars = Math.min(members.size(), 3);
                    int sizePx = (int) (24 * itemView.getResources().getDisplayMetrics().density);
                    int overlapPx = (int) (12 * itemView.getResources().getDisplayMetrics().density);

                    for (int i = 0; i < maxAvatars; i++) {
                        ImageView iv = new ImageView(itemView.getContext());
                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizePx, sizePx);
                        params.setMargins(i * overlapPx, 0, 0, 0);
                        iv.setLayoutParams(params);
                        iv.setBackgroundResource(R.drawable.circle_avatar);
                        iv.setPadding(2, 2, 2, 2);
                        iv.setClipToOutline(true);
                        iv.setImageResource(R.drawable.ic_person);
                        flAvatarStack.addView(iv);
                    }
                }
            }

            if (btnJoin != null) {
                btnJoin.setVisibility(isDiscoverMode ? View.VISIBLE : View.GONE);
                if (isDiscoverMode) {
                    boolean alreadyMember = isMemberOf(group, currentUserId);
                    if (alreadyMember) {
                        btnJoin.setText("Abonné ✓");
                        btnJoin.setEnabled(true);
                        btnJoin.setTextColor(Color.parseColor("#4CAF50"));
                        btnJoin.setOnClickListener(v -> {
                            if (listener != null) listener.onLeaveClick(group);
                        });
                    } else {
                        btnJoin.setText("Rejoindre");
                        btnJoin.setEnabled(true);
                        btnJoin.setTextColor(Color.parseColor("#7C3AED"));
                        btnJoin.setOnClickListener(v -> {
                            if (listener != null) listener.onJoinClick(group);
                        });
                    }
                }
            }

            if (btnDelete != null) {
                boolean isCreator = currentUserId != null && group.getCreator() != null && currentUserId.equals(group.getCreator().getId());
                btnDelete.setVisibility(!isDiscoverMode && isCreator ? View.VISIBLE : View.GONE);
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(group);
                });
            }

            itemView.setOnClickListener(v -> {
                if (!isDiscoverMode && listener != null) {
                    listener.onGroupClick(group);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onGroupLongClick(group);
                    return true;
                }
                return false;
            });
        }

        private static boolean isMemberOf(Group group, String userId) {
            if (userId == null || group.getMembers() == null) return false;
            for (Group.Member m : group.getMembers()) {
                if (userId.equals(m.getId())) return true;
            }
            return false;
        }
    }
}
