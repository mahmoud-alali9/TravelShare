package com.example.travelshare.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelshare.R;
import com.example.travelshare.model.Comment;
import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentDeleteListener {
        void onDelete(Comment comment);
    }

    private List<Comment> comments = new ArrayList<>();
    private OnCommentDeleteListener deleteListener;
    private String currentUsername;

    public void setComments(List<Comment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        this.comments.add(0, comment);
        notifyItemInserted(0);
    }

    public void removeComment(Comment comment) {
        int pos = comments.indexOf(comment);
        if (pos != -1) {
            comments.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public void setDeleteListener(OnCommentDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.tvText.setText(comment.getText());
        holder.tvAuthor.setText(comment.getUser().getFullName());
        holder.tvUsername.setText("@" + comment.getUser().getUsername());
        
        // Date simplifiée (ex: 12/04/2026)
        if (comment.getCreatedAt() != null && comment.getCreatedAt().length() >= 10) {
            holder.tvDate.setText(comment.getCreatedAt().substring(0, 10));
        }

        // Avatar Initial
        String initial = comment.getUser().getFullName().substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);

        // Bouton supprimer visible si c'est notre commentaire
        boolean isMine = currentUsername != null && currentUsername.equals(comment.getUser().getUsername());
        holder.btnDelete.setVisibility(isMine ? View.VISIBLE : View.GONE);
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(comment);
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvAuthor, tvUsername, tvText, tvDate;
        ImageButton btnDelete;

        CommentViewHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tv_comment_avatar);
            tvAuthor = v.findViewById(R.id.tv_comment_author);
            tvUsername = v.findViewById(R.id.tv_comment_username);
            tvText = v.findViewById(R.id.tv_comment_text);
            tvDate = v.findViewById(R.id.tv_comment_date);
            btnDelete = v.findViewById(R.id.btn_delete_comment);
        }
    }
}
