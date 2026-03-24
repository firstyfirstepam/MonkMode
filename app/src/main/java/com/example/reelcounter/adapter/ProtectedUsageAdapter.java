package com.example.reelcounter.adapter;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reelcounter.R;
import com.example.reelcounter.data.ProtectedUsageLoader;
import com.example.reelcounter.model.ProtectedUsageSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Row: icon, app name, time in foreground, open count.
 */
public final class ProtectedUsageAdapter extends RecyclerView.Adapter<ProtectedUsageAdapter.Holder> {

    private final List<ProtectedUsageSummary> items = new ArrayList<>();
    private final PackageManager packageManager;

    public ProtectedUsageAdapter(@NonNull PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    public void setItems(@NonNull List<ProtectedUsageSummary> next) {
        items.clear();
        items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_protected_usage, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ProtectedUsageSummary s = items.get(position);
        holder.title.setText(s.getLabel());
        holder.subtitle.setText(ProtectedUsageLoader.formatDurationShort(
                s.getTotalTimeForegroundMs(),
                holder.itemView.getContext()
        ));
        holder.opens.setText(ProtectedUsageLoader.formatOpens(
                s.getForegroundOpenCount(),
                holder.itemView.getContext()
        ));

        try {
            Drawable d = packageManager.getApplicationIcon(s.getPackageName());
            holder.icon.setImageDrawable(d);
        } catch (PackageManager.NameNotFoundException e) {
            holder.icon.setImageResource(R.mipmap.ic_launcher);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;
        final TextView opens;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.usage_app_icon);
            title = itemView.findViewById(R.id.usage_app_title);
            subtitle = itemView.findViewById(R.id.usage_time);
            opens = itemView.findViewById(R.id.usage_opens);
        }
    }
}
