package com.example.adaptivesoundscapemooddiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MoodEntryAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicjalizacja bazy danych
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "mood-diary-db").build();

        // Pobranie danych w tle
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
            this.entries = entries;
        }

        @Override
        public MoodEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mood_entry, parent, false);
            return new MoodEntryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MoodEntryViewHolder holder, int position) {
            MoodEntry entry = entries.get(position);

            // Formatowanie daty
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            holder.dateTextView.setText(sdf.format(entry.getDate()));

            holder.moodTextView.setText(entry.getDetectedMood());
            holder.trackTextView.setText(entry.getSpotifyTrackName());

            // Ładowanie zdjęcia jeśli istnieje
            if (entry.getSelfieUri() != null && !entry.getSelfieUri().isEmpty()) {
                holder.selfieImageView.setImageURI(Uri.parse(entry.getSelfieUri()));
            } else {
                holder.selfieImageView.setImageResource(android.R.drawable.ic_menu_camera);
            }

            // Obsługa kliknięcia na utwór
            holder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(entry.getSpotifyUri()));
                    intent.putExtra(Intent.EXTRA_REFERRER,
                            Uri.parse("android-app://" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    // Obsługa błędu
                }
            });
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class MoodEntryViewHolder extends RecyclerView.ViewHolder {
            TextView dateTextView, moodTextView, trackTextView;
            ImageView selfieImageView;

            public MoodEntryViewHolder(View itemView) {
                super(itemView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
                moodTextView = itemView.findViewById(R.id.moodTextView);
                trackTextView = itemView.findViewById(R.id.trackTextView);
                selfieImageView = itemView.findViewById(R.id.selfieImageView);
            }
        }
    }
}
