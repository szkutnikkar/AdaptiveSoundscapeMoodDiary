package com.example.adaptivesoundscapemooddiary;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MoodEntryAdapter adapter;
    private AppDatabase db;
    private boolean isDeleteMode = false;
    private Set<Integer> selectedItems = new HashSet<>();
    private ImageButton backButton;
    private ImageButton deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Inicjalizacja przycisków
        backButton = findViewById(R.id.backButton);
        deleteButton = findViewById(R.id.deleteButton);
        backButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> handleDeleteButtonClick());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicjalizacja bazy danych
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "mood-diary-db")
                .fallbackToDestructiveMigration()
                .build();

        loadEntries();
    }

    private void handleDeleteButtonClick() {
        if (!isDeleteMode) {
            startDeleteMode();
        } else if (selectedItems.isEmpty()) {
            exitDeleteMode();
        } else {
            showDeleteConfirmationDialog();
        }
    }

    private void startDeleteMode() {
        isDeleteMode = true;
        selectedItems.clear();
        deleteButton.setImageResource(R.drawable.ic_delete_forever);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        Toast.makeText(this, "Wybierz wpisy do usunięcia", Toast.LENGTH_SHORT).show();
    }

    private void exitDeleteMode() {
        isDeleteMode = false;
        selectedItems.clear();
        deleteButton.setImageResource(R.drawable.ic_delete);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showDeleteConfirmationDialog() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Najpierw wybierz wpisy do usunięcia", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Potwierdzenie usunięcia")
                .setMessage("Czy na pewno chcesz usunąć wybrane wpisy?")
                .setPositiveButton("Usuń", (dialog, which) -> {
                    deleteSelectedEntries();
                })
                .setNegativeButton("Anuluj", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void deleteSelectedEntries() {
        if (selectedItems.isEmpty() || adapter == null || adapter.entries == null) {
            return;
        }

        new Thread(() -> {
            try {
                List<MoodEntry> entriesToDelete = new ArrayList<>();
                for (MoodEntry entry : adapter.entries) {
                    if (selectedItems.contains(entry.getId())) {
                        entriesToDelete.add(entry);
                    }
                }
                
                for (MoodEntry entry : entriesToDelete) {
                    db.moodEntryDao().delete(entry);
                }

                runOnUiThread(() -> {
                    adapter.entries.removeAll(entriesToDelete);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Wpisy zostały usunięte", Toast.LENGTH_SHORT).show();
                    exitDeleteMode();
                    
                    // Wysyłamy broadcast o zmianie w bazie danych
                    Intent intent = new Intent("com.example.adaptivesoundscapemooddiary.DATABASE_CHANGED");
                    sendBroadcast(intent);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Błąd podczas usuwania wpisów", Toast.LENGTH_SHORT).show();
                    exitDeleteMode();
                });
            }
        }).start();
    }

    private void loadEntries() {
        new Thread(() -> {
            List<MoodEntry> entries = db.moodEntryDao().getAllEntries();
            runOnUiThread(() -> {
                adapter = new MoodEntryAdapter(entries);
                recyclerView.setAdapter(adapter);
            });
        }).start();
    }

    private class MoodEntryAdapter extends RecyclerView.Adapter<MoodEntryAdapter.MoodEntryViewHolder> {
        private List<MoodEntry> entries;

        public MoodEntryAdapter(List<MoodEntry> entries) {
            this.entries = entries != null ? entries : new ArrayList<>();
        }

        @NonNull
        @Override
        public MoodEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mood_entry, parent, false);
            return new MoodEntryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MoodEntryViewHolder holder, int position) {
            MoodEntry entry = entries.get(position);

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            holder.dateTextView.setText(sdf.format(entry.getDate()));
            holder.moodTextView.setText("Nastrój: " + entry.getDetectedMood());
            holder.trackTextView.setText("Utwór: " + entry.getSpotifyTrackName());

            holder.selectCheckBox.setVisibility(isDeleteMode ? View.VISIBLE : View.GONE);
            holder.selectCheckBox.setChecked(selectedItems.contains(entry.getId()));
            holder.itemView.setSelected(selectedItems.contains(entry.getId()));

            if (entry.getSelfieUri() != null && !entry.getSelfieUri().isEmpty()) {
                holder.selfieImageView.setImageURI(Uri.parse(entry.getSelfieUri()));
                holder.selfieImageView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.history_image_background));
                holder.selfieImageView.setVisibility(View.VISIBLE);
            } else {
                holder.selfieImageView.setBackground(null);
                holder.selfieImageView.setImageResource(android.R.drawable.ic_menu_camera);
                holder.selfieImageView.setVisibility(View.GONE);
            }

            View.OnClickListener clickListener = v -> {
                if (isDeleteMode) {
                    toggleSelection(entry.getId());
                    holder.selectCheckBox.setChecked(selectedItems.contains(entry.getId()));
                    holder.itemView.setSelected(selectedItems.contains(entry.getId()));
                } else {
                    try {
                        String spotifyUri = entry.getSpotifyUri();
                        if (spotifyUri != null && !spotifyUri.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(spotifyUri));
                            intent.putExtra(Intent.EXTRA_REFERRER,
                                    Uri.parse("android-app://" + holder.itemView.getContext().getPackageName()));
                            holder.itemView.getContext().startActivity(intent);
                        } else {
                            Toast.makeText(holder.itemView.getContext(), "Brak przypisanego utworu", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(holder.itemView.getContext(), "Błąd przy otwieraniu Spotify", Toast.LENGTH_SHORT).show();
                    }
                }
            };

            holder.itemView.setOnClickListener(clickListener);
            holder.selectCheckBox.setOnClickListener(v -> {
                toggleSelection(entry.getId());
                holder.itemView.setSelected(selectedItems.contains(entry.getId()));
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (!isDeleteMode) {
                    startDeleteMode();
                    toggleSelection(entry.getId());
                    holder.selectCheckBox.setChecked(selectedItems.contains(entry.getId()));
                    holder.itemView.setSelected(selectedItems.contains(entry.getId()));
                    return true;
                }
                return false;
            });
        }

        private void toggleSelection(int entryId) {
            if (selectedItems.contains(entryId)) {
                selectedItems.remove(entryId);
            } else {
                selectedItems.add(entryId);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class MoodEntryViewHolder extends RecyclerView.ViewHolder {
            final TextView dateTextView;
            final TextView moodTextView;
            final TextView trackTextView;
            final ImageView selfieImageView;
            final CheckBox selectCheckBox;

            MoodEntryViewHolder(View itemView) {
                super(itemView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
                moodTextView = itemView.findViewById(R.id.moodTextView);
                trackTextView = itemView.findViewById(R.id.trackTextView);
                selfieImageView = itemView.findViewById(R.id.selfieImageView);
                selectCheckBox = itemView.findViewById(R.id.selectCheckBox);
            }
        }
    }
}
