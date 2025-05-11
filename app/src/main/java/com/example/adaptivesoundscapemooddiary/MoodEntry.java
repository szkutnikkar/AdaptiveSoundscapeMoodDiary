package com.example.adaptivesoundscapemooddiary;


import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "mood_entries")
public class MoodEntry {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private Date date;
    private String moodText;
    private String detectedMood;
    private String selfieUri;
    private String spotifyUri;
    private String spotifyTrackName;


    // Konstruktor, gettery i settery
    public MoodEntry(Date date, String moodText, String detectedMood, String selfieUri, String spotifyUri, String spotifyTrackName) {
        this.date = date;
        this.moodText = moodText;
        this.detectedMood = detectedMood;
        this.selfieUri = selfieUri;
        this.spotifyUri = spotifyUri;
        this.spotifyTrackName = spotifyTrackName;

    }



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMoodText() {
        return moodText;
    }

    public void setMoodText(String moodText) {
        this.moodText = moodText;
    }

    public String getDetectedMood() {
        return detectedMood;
    }

    public void setDetectedMood(String detectedMood) {
        this.detectedMood = detectedMood;
    }

    public String getSelfieUri() {
        return selfieUri;
    }

    public void setSelfieUri(String selfieUri) {
        this.selfieUri = selfieUri;
    }

    public String getSpotifyUri() {
        return spotifyUri;
    }

    public void setSpotifyUri(String spotifyUri) {
        this.spotifyUri = spotifyUri;
    }

    public String getSpotifyTrackName() {
        return spotifyTrackName;
    }

    public void setSpotifyTrackName(String spotifyTrackName) {
        this.spotifyTrackName = spotifyTrackName;
    }

}
