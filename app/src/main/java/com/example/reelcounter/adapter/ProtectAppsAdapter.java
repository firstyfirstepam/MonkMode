package com.example.reelcounter.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reelcounter.R;
import com.example.reelcounter.data.PreferencesManager;
import com.example.reelcounter.model.AppInfo;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * List of apps with a switch to toggle friction (strict mode) protection.
 */
public final class ProtectAppsAdapter extends RecyclerView.Adapter<ProtectAppsAdapter.Holder> {

    private final List<AppInfo> apps = new ArrayList<>();
    private final PreferencesManager preferencesManager;

    public ProtectAppsAdapter(@NonNull PreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
    }

    public void setApps(@NonNull List<AppInfo> next) {
        apps.clear();
        apps.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_protect_app, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.getIcon());
        holder.label.setText(app.getLabel());
        String pkg = app.getPackageName();

        holder.switchView.setOnCheckedChangeListener(null);
        holder.switchView.setChecked(preferencesManager.isFrictionProtected(pkg));
        holder.switchView.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferencesManager.setFrictionProtected(pkg, isChecked));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final SwitchMaterial switchView;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.protect_app_icon);
            label = itemView.findViewById(R.id.protect_app_label);
            switchView = itemView.findViewById(R.id.protect_app_switch);
        }
    }
}
