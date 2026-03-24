package com.example.reelcounter.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reelcounter.R;
import com.example.reelcounter.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid of launchable apps (icon + label).
 */
public final class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.Holder> {

    public interface Listener {
        void onAppClicked(@NonNull AppInfo app);

        void onAppLongPressed(@NonNull AppInfo app);
    }

    private final List<AppInfo> apps = new ArrayList<>();
    private final Listener listener;
    private final java.util.Set<String> protectedPackages;

    public AppGridAdapter(@NonNull Listener listener, @NonNull java.util.Set<String> protectedPackages) {
        this.listener = listener;
        this.protectedPackages = protectedPackages;
    }

    public void setApps(@NonNull List<AppInfo> next) {
        apps.clear();
        apps.addAll(next);
        notifyDataSetChanged();
    }

    public void setProtectedPackages(@NonNull java.util.Set<String> next) {
        protectedPackages.clear();
        protectedPackages.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.getIcon());
        holder.label.setText(app.getLabel());
        boolean prot = protectedPackages.contains(app.getPackageName());
        holder.badge.setVisibility(prot ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onAppClicked(app));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onAppLongPressed(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView badge;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
            badge = itemView.findViewById(R.id.protected_badge);
        }
    }
}
