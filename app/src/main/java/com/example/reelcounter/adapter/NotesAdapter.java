package com.example.reelcounter.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reelcounter.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple list of user-defined regret/reminder notes.
 */
public final class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.Holder> {

    public interface Listener {
        void onDeleteNote(int index);
    }

    private final List<String> notes = new ArrayList<>();
    private final Listener listener;

    public NotesAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setNotes(@NonNull List<String> next) {
        notes.clear();
        notes.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.text.setText(notes.get(position));
        holder.delete.setOnClickListener(v -> listener.onDeleteNote(position));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView text;
        final ImageButton delete;

        Holder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.note_text);
            delete = itemView.findViewById(R.id.note_delete);
        }
    }
}
