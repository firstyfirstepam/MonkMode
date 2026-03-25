package com.example.reelcounter;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reelcounter.adapter.NotesAdapter;
import com.example.reelcounter.data.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * Create and delete custom regret/reminder notes shown randomly on the friction screen.
 */
public class NotesActivity extends BaseThemedActivity implements NotesAdapter.Listener {

    private PreferencesManager preferencesManager;
    private NotesAdapter adapter;
    private TextInputEditText input;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        preferencesManager = new PreferencesManager(this);

        MaterialToolbar toolbar = findViewById(R.id.notes_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        input = findViewById(R.id.note_input);
        empty = findViewById(R.id.notes_empty);
        MaterialButton add = findViewById(R.id.note_add_button);
        RecyclerView list = findViewById(R.id.notes_list);

        adapter = new NotesAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        add.setOnClickListener(v -> {
            if (input.getText() == null) {
                return;
            }
            preferencesManager.addNote(input.getText().toString());
            input.setText("");
            refresh();
        });

        refresh();
    }

    private void refresh() {
        List<String> notes = preferencesManager.getNotes();
        adapter.setNotes(notes);
        empty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDeleteNote(int index) {
        preferencesManager.removeNoteAtIndex(index);
        refresh();
    }
}
