package com.example.adaptivesoundscapemooddiary;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MoodEntryDao {
    @Insert
    void insert(MoodEntry moodEntry);
    @Query("SELECT * FROM mood_entries")
    List<MoodEntry> getAllEntries();

    @Query("DELETE FROM mood_entries WHERE id = :id")
    void deleteEntry(int id);

    @Query("SELECT * FROM mood_entries ORDER BY date DESC LIMIT 1")
    MoodEntry getLastEntry();
}
