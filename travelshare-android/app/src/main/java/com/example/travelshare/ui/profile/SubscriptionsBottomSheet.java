package com.example.travelshare.ui.profile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelshare.R;
import com.example.travelshare.model.Subscription;
import com.example.travelshare.network.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionsBottomSheet extends BottomSheetDialogFragment {

    private ProfileViewModel  viewModel;
    private TokenManager      tokenManager;
    private SubscriptionAdapter adapter;
    private TextView          tvEmpty;
    private RecyclerView      rv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_subscriptions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel    = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        tokenManager = new TokenManager(requireContext());

        tvEmpty = view.findViewById(R.id.tv_no_subscriptions);
        rv      = view.findViewById(R.id.rv_subscriptions);

        adapter = new SubscriptionAdapter(sub -> {
            viewModel.unsubscribe(sub.getType(), sub.getTargetId(), new ProfileViewModel.OnUnsubscribeListener() {
                @Override public void onSuccess() {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Désabonné", Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(String message) {
                    if (getContext() != null)
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        viewModel.getSubscriptions().observe(getViewLifecycleOwner(), subs -> {
            boolean empty = subs == null || subs.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            rv.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (!empty) adapter.setData(subs);
        });

        viewModel.loadSubscriptions();
    }

    // ── Adapter interne ─────────────────────────────────────
    static class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.VH> {

        interface OnUnsubscribeClick { void onClick(Subscription sub); }

        private List<Subscription>    data     = new ArrayList<>();
        private final OnUnsubscribeClick listener;

        SubscriptionAdapter(OnUnsubscribeClick listener) { this.listener = listener; }

        void setData(List<Subscription> subs) {
            data = new ArrayList<>(subs);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subscription, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Subscription sub = data.get(position);
            h.tvType.setText(typeLabel(sub.getType()));
            h.tvType.setBackgroundTintList(ColorStateList.valueOf(typeColor(sub.getType())));
            h.tvTarget.setText(sub.getDisplayName());
            h.btnUnsubscribe.setOnClickListener(v -> listener.onClick(sub));
        }

        @Override public int getItemCount() { return data.size(); }

        private String typeLabel(String type) {
            if (type == null) return "?";
            switch (type) {
                case "author":   return "Auteur";
                case "group":    return "Groupe";
                case "location": return "Lieu";
                case "theme":    return "Thème";
                default:         return type;
            }
        }

        private int typeColor(String type) {
            if (type == null) return Color.GRAY;
            switch (type) {
                case "author":   return Color.parseColor("#1565C0");
                case "group":    return Color.parseColor("#E65100");
                case "location": return Color.parseColor("#2E7D32");
                case "theme":    return Color.parseColor("#6A1B9A");
                default:         return Color.GRAY;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView    tvType, tvTarget;
            ImageButton btnUnsubscribe;
            VH(View v) {
                super(v);
                tvType        = v.findViewById(R.id.tv_sub_type);
                tvTarget      = v.findViewById(R.id.tv_sub_target);
                btnUnsubscribe = v.findViewById(R.id.btn_unsubscribe);
            }
        }
    }
}
