package com.example.adaptivesoundscapemooddiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.ImageButton;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView emotionTextView, questionTextView, resultTextView;
    private FaceEmotionDetector faceEmotionDetector;
    private String detectedEmotion = "Nie wykryto";
    private BroadcastReceiver databaseChangeReceiver;

    private RadioGroup answerRadioGroup;
    private Button nextButton, takePhotoButton, restartButton, viewHistoryButton;
    private ImageView selfieImageView;

    private LinearLayout questionnaireContainer, resultContainer;

    private LinearLayout lastEntryContainer;
    private ImageView lastEntryImage;
    private TextView lastEntryDate, lastEntryMood, lastEntrySong;
    private Button startQuestionnaireButton;

    private int currentQuestion = 0;
    private int totalScore = 0;
    private String selfieUri = null;

    private AppDatabase db;

    private String[] questions = {
            "Jak się dziś czujesz?",
            "Co najlepiej opisuje Twój dzisiejszy poziom energii?",
            "Jak oceniasz swoją motywację do działania?",
            "Które zdanie najlepiej opisuje Twoje myśli?",
            "Jak spałeś/aś ostatniej nocy?"
    };

    private String[][] answers = {
            {"Świetnie!", "Dobrze", "Neutralnie", "Nie najlepiej", "Źle"},
            {"Pełen/na energii", "Dość energiczny/a", "Normalnie", "Trochę zmęczony/a", "Bardzo zmęczony/a"},
            {"Chcę góry przenosić!", "Mam chęć do działania", "Jest OK", "Trudno się zmotywować", "Brak motywacji"},
            {"Pozytywne i pełne nadziei", "Spokojne i zrównoważone", "Neutralne", "Lekko niepokojące", "Negatywne i przygnębiające"},
            {"Świetnie, jestem wypoczęty/a", "Całkiem dobrze", "Przeciętnie", "Słabo", "Bardzo źle"}
    };

    private int[] answerScores = {2, 1, 0, -1, -2}; // Punktacja dla każdej odpowiedzi

    private static final Map<String, SpotifyTrack> MOOD_TO_SPOTIFY = new HashMap<String, SpotifyTrack>() {{
        // Podstawowe nastroje
        put("szczęśliwy/a", new SpotifyTrack("spotify:track:60nZcImufyMA1MKQY3dcCH", "Happy - Pharrell Williams"));
        put("bardzo szczęśliwy/a", new SpotifyTrack("spotify:track:60nZcImufyMA1MKQY3dcCH", "Happy - Pharrell Williams"));
        put("smutny/a", new SpotifyTrack("spotify:track:3ee8Jmje8o58CHK66QrVC2", "Sad! - XXXTENTACION"));
        put("wyraźnie smutny/a", new SpotifyTrack("spotify:track:3ee8Jmje8o58CHK66QrVC2", "Sad! - XXXTENTACION"));
        put("zestresowany/a", new SpotifyTrack("spotify:track:3SXzmfGtwCdFkms7gEdHC2", "Cry OF The Unheard - REPULSIVE"));
        put("spokojny/a", new SpotifyTrack("spotify:track:5F3qrwxuHSz88vXBGh3tNy", "Interstellar - Calmly"));
        put("zmęczony/a", new SpotifyTrack("spotify:track:7H0ya83CMmgFcOhw0UB6ow", "Space Song - Beach House"));
        put("zły/a", new SpotifyTrack("spotify:track:5cZqsjVs6MevCnAkasbEOX?", "Break Stuff - Limp Bizkit"));
        put("bardzo zły/a", new SpotifyTrack("spotify:track:5cZqsjVs6MevCnAkasbEOX?", "Break Stuff - Limp Bizkit"));
        put("neutralny/a", new SpotifyTrack("spotify:track:6kkwzB6hXLIONkEk9JciA6?", "Weightless - Marconi Union"));
        put("stabilny/a neutralny/a", new SpotifyTrack("spotify:track:6kkwzB6hXLIONkEk9JciA6?", "Weightless - Marconi Union"));
    }};

    // Add new card view references
    private MaterialCardView questionnaireCard;
    private MaterialCardView resultCard;
    private MaterialCardView lastEntryCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupDatabase();
        setupListeners();
        setupDatabaseChangeReceiver();
        loadLastEntry();
    }

    private void initializeViews() {
        // Inicjalizacja widoków
        faceEmotionDetector = new FaceEmotionDetector();
        
        // Initialize card views
        questionnaireCard = findViewById(R.id.questionnaireCard);
        resultCard = findViewById(R.id.resultCard);
        lastEntryCard = findViewById(R.id.lastEntryCard);
        
        // Główne widoki
        questionTextView = findViewById(R.id.questionTextView);
        answerRadioGroup = findViewById(R.id.answerRadioGroup);
        nextButton = findViewById(R.id.nextButton);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        selfieImageView = findViewById(R.id.selfieImageView);
        emotionTextView = findViewById(R.id.emotionTextView);
        
        // Dodaj obsługę przycisku zamykania
        ImageButton closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> showExitConfirmationDialog());
        
        // Kontenery
        questionnaireContainer = findViewById(R.id.questionnaireContainer);
        resultContainer = findViewById(R.id.resultContainer);
        lastEntryContainer = findViewById(R.id.lastEntryContainer);
        
        // Przyciski
        startQuestionnaireButton = findViewById(R.id.startQuestionnaireButton);
        restartButton = findViewById(R.id.restartButton);
        viewHistoryButton = findViewById(R.id.viewHistoryButton);
        
        // Teksty wyników
        resultTextView = findViewById(R.id.resultTextView);
        lastEntryImage = findViewById(R.id.lastEntryImage);
        lastEntryDate = findViewById(R.id.lastEntryDate);
        lastEntryMood = findViewById(R.id.lastEntryMood);
        lastEntrySong = findViewById(R.id.lastEntrySong);

        // Ustawienie początkowej widoczności
        questionnaireCard.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        questionnaireContainer.setVisibility(View.VISIBLE);
        resultContainer.setVisibility(View.VISIBLE);
        startQuestionnaireButton.setVisibility(View.VISIBLE);
    }

    private void setupDatabase() {
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "mood-diary-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    private void setupListeners() {
        startQuestionnaireButton.setOnClickListener(v -> startQuestionnaire());
        nextButton.setOnClickListener(v -> handleNextQuestion());
        takePhotoButton.setOnClickListener(v -> takeSelfie());
        restartButton.setOnClickListener(v -> resetQuestionnaire());
        viewHistoryButton.setOnClickListener(v -> openHistory());
    }

    private void setupDatabaseChangeReceiver() {
        databaseChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.adaptivesoundscapemooddiary.DATABASE_CHANGED".equals(intent.getAction())) {
                    loadLastEntry();
                }
            }
        };
        registerReceiver(databaseChangeReceiver, new IntentFilter("com.example.adaptivesoundscapemooddiary.DATABASE_CHANGED"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseChangeReceiver != null) {
            unregisterReceiver(databaseChangeReceiver);
        }
    }

    private void startQuestionnaire() {
        currentQuestion = 0;
        totalScore = 0;
        answerRadioGroup.clearCheck();
        
        // Pokaż kartę z ankietą
        questionnaireCard.setVisibility(View.VISIBLE);
        questionnaireContainer.setVisibility(View.VISIBLE);
        takePhotoButton.setVisibility(View.VISIBLE);
        
        // Ukryj pozostałe elementy
        startQuestionnaireButton.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        
        // Pokaż pierwsze pytanie
        showQuestion(currentQuestion);
    }

    private void handleNextQuestion() {
        int selectedId = answerRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Wybierz odpowiedź", Toast.LENGTH_SHORT).show();
            return;
        }

        int answerIndex = 0;
        if (selectedId == R.id.option1) answerIndex = 0;
        else if (selectedId == R.id.option2) answerIndex = 1;
        else if (selectedId == R.id.option3) answerIndex = 2;
        else if (selectedId == R.id.option4) answerIndex = 3;
        else if (selectedId == R.id.option5) answerIndex = 4;

        totalScore += answerScores[answerIndex];

        if (currentQuestion < questions.length - 1) {
            currentQuestion++;
            showQuestion(currentQuestion);
            answerRadioGroup.clearCheck();
        } else {
            finishQuestionnaire();
        }
    }

    private void takeSelfie() {
        ImagePicker.with(MainActivity.this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start();
    }

    private void openHistory() {
        startActivity(new Intent(this, HistoryActivity.class));
    }

    private void showQuestion(int questionIndex) {
        if (questionIndex < 0 || questionIndex >= questions.length) {
            Toast.makeText(this, "Błąd: Nieprawidłowy indeks pytania", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ustaw tekst pytania
        questionTextView.setText(questions[questionIndex]);

        // Znajdź przyciski radiowe
        RadioButton option1 = findViewById(R.id.option1);
        RadioButton option2 = findViewById(R.id.option2);
        RadioButton option3 = findViewById(R.id.option3);
        RadioButton option4 = findViewById(R.id.option4);
        RadioButton option5 = findViewById(R.id.option5);

        // Ustaw odpowiednie etykiety dla przycisków
        if (option1 != null && option2 != null && option3 != null && option4 != null && option5 != null) {
            option1.setText(answers[questionIndex][0]);
            option2.setText(answers[questionIndex][1]);
            option3.setText(answers[questionIndex][2]);
            option4.setText(answers[questionIndex][3]);
            option5.setText(answers[questionIndex][4]);
        }

        // Aktualizuj tekst przycisku
        nextButton.setText(questionIndex == questions.length - 1 ? "Zakończ ankietę" : "Następne pytanie");
    }

    private void showResult(String mood, int score) {
        // Ukryj niepotrzebne elementy
        selfieImageView.setVisibility(View.GONE);
        questionnaireContainer.setVisibility(View.GONE);
        questionnaireCard.setVisibility(View.GONE);
        startQuestionnaireButton.setVisibility(View.GONE);

        // Pokaż wyniki
        resultCard.setVisibility(View.VISIBLE);
        resultContainer.setVisibility(View.VISIBLE);
        resultTextView.setText("Twój nastrój: " + mood);
        
        // Upewnij się, że przycisk do ponownego wypełnienia jest widoczny
        restartButton.setVisibility(View.VISIBLE);
    }

    private void resetQuestionnaire() {
        // Ukryj karty
        questionnaireCard.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        takePhotoButton.setVisibility(View.GONE);
        
        // Pokaż przycisk rozpoczęcia
        startQuestionnaireButton.setVisibility(View.VISIBLE);
        
        // Reset zmiennych
        currentQuestion = 0;
        totalScore = 0;
        selfieUri = null;
        detectedEmotion = "Nie wykryto";
        
        // Wyczyść widoki
        selfieImageView.setVisibility(View.GONE);
        emotionTextView.setVisibility(View.GONE);
        emotionTextView.setText("");
        resultContainer.setVisibility(View.GONE);
        answerRadioGroup.clearCheck();
        
        // Ukryj ostatni wpis i wyczyść jego zawartość
        lastEntryContainer.setVisibility(View.GONE);
        lastEntryDate.setText("");
        lastEntryMood.setText("");
        lastEntrySong.setText("");
        lastEntryImage.setImageURI(null);
        lastEntryImage.setVisibility(View.GONE);
        
        // Załaduj ostatni wpis tylko jeśli istnieje w bazie
        loadLastEntry();
    }

    private void finishQuestionnaire() {
        if (questions.length == 0) {
            Toast.makeText(this, "Błąd: brak pytań", Toast.LENGTH_SHORT).show();
            return;
        }
        String detectedMood = analyzeMood(totalScore);
        String finalMood = combineMoods(detectedMood, detectedEmotion);
        
        // Szukamy piosenki najpierw dla pełnego nastroju
        String searchMood = finalMood.toLowerCase();
        final SpotifyTrack finalSpotifyTrack;
        SpotifyTrack spotifyTrack = MOOD_TO_SPOTIFY.get(searchMood);
        
        // Jeśli nie znaleziono, szukamy dla podstawowego nastroju
        if (spotifyTrack == null) {
            spotifyTrack = MOOD_TO_SPOTIFY.get(detectedMood.toLowerCase());
        }
        
        // Jeśli nadal nie znaleziono, używamy domyślnej piosenki
        if (spotifyTrack == null) {
            spotifyTrack = new SpotifyTrack("spotify:track:0Dg5BZ2tkBUkUYIMWbc4wG", "Default Track");
        }
        
        finalSpotifyTrack = spotifyTrack;

        MoodEntry newEntry = new MoodEntry(
                new Date(),
                "",  // Usuwamy wynik punktowy
                finalMood,
                selfieUri,
                finalSpotifyTrack.uri,
                finalSpotifyTrack.trackName
        );

        new Thread(() -> {
            db.moodEntryDao().insert(newEntry);

            runOnUiThread(() -> {
                // Ukryj elementy ankiety
                emotionTextView.setVisibility(View.GONE);
                emotionTextView.setText("");
                selfieImageView.setVisibility(View.GONE);
                questionnaireContainer.setVisibility(View.GONE);
                questionnaireCard.setVisibility(View.GONE);
                startQuestionnaireButton.setVisibility(View.GONE);
                takePhotoButton.setVisibility(View.GONE);

                // Pokaż wyniki
                showResult(detectedMood, totalScore);
                
                // Załaduj i pokaż ostatni wpis
                loadLastEntry();
                lastEntryContainer.setVisibility(View.VISIBLE);

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(finalSpotifyTrack.uri)));
                } catch (Exception e) {
                    Toast.makeText(this, "Błąd przy otwieraniu Spotify", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private String analyzeMood(int score) {
        if (score >= 7) return "szczęśliwy/a";
        else if (score >= 4) return "spokojny/a";
        else if (score >= 0) return "neutralny/a";
        else if (score >= -4) return "smutny/a";
        else if (score >= -7) return "zły/a";
        else return "zestresowany/a";
    }

    private void loadLastEntry() {
        new Thread(() -> {
            MoodEntry lastEntry = db.moodEntryDao().getLastEntry();
            runOnUiThread(() -> {
                if (lastEntry != null) {
                    String dateText = lastEntry.getDate() != null ?
                            DateFormat.getDateInstance().format(lastEntry.getDate()) : "Brak daty";
                    lastEntryDate.setText("Data: " + dateText);
                    lastEntryMood.setText("Nastrój: " + lastEntry.getDetectedMood());
                    lastEntrySong.setText("Utwór: " + lastEntry.getSpotifyTrackName());

                    if (lastEntry.getSelfieUri() != null) {
                        lastEntryImage.setImageURI(Uri.parse(lastEntry.getSelfieUri()));
                        lastEntryImage.setVisibility(View.VISIBLE);
                    } else {
                        lastEntryImage.setVisibility(View.GONE);
                    }
                    lastEntryContainer.setVisibility(View.VISIBLE);
                    lastEntryCard.setVisibility(View.VISIBLE);
                    // Show start button only if result card is not visible
                    startQuestionnaireButton.setVisibility(resultCard.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                } else {
                    lastEntryContainer.setVisibility(View.GONE);
                    lastEntryCard.setVisibility(View.GONE);
                    // Only show start button if we're not in the result view
                    startQuestionnaireButton.setVisibility(resultCard.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    // Reset all text views to prevent showing old data
                    lastEntryDate.setText("");
                    lastEntryMood.setText("");
                    lastEntrySong.setText("");
                    lastEntryImage.setImageURI(null);
                    lastEntryImage.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            selfieUri = uri.toString();
            selfieImageView.setImageURI(uri);
            selfieImageView.setVisibility(View.VISIBLE);

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                if (bitmap != null) {
                    faceEmotionDetector.detectEmotions(bitmap, new FaceEmotionDetector.EmotionDetectionCallback() {
                        @Override
                        public void onEmotionDetected(String emotion) {
                            runOnUiThread(() -> {
                                detectedEmotion = (emotion != null && !emotion.isEmpty()) ? emotion : "Nie wykryto";
                                emotionTextView.setText("Wykryta emocja: " + detectedEmotion);
                                emotionTextView.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> {
                                detectedEmotion = "Nie wykryto";
                                emotionTextView.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Błąd detekcji: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    detectedEmotion = "Nie wykryto";
                    Toast.makeText(MainActivity.this, "Błąd przetwarzania zdjęcia", Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            detectedEmotion = "Nie wykryto";
        }
    }

    private String combineMoods(String quizMood, String faceEmotion) {
        if (quizMood == null || quizMood.isEmpty()) {
            return faceEmotion != null ? faceEmotion : "Nie określono";
        }

        if (faceEmotion == null || faceEmotion.isEmpty() || faceEmotion.equals("Nie wykryto")) {
            return quizMood;
        }

        String lowerFaceEmotion = faceEmotion.toLowerCase().trim();
        String lowerQuizMood = quizMood.toLowerCase().trim();

        if (lowerFaceEmotion.contains("radosn") && lowerQuizMood.contains("szczęśliwy/a")) {
            return "Bardzo szczęśliwy/a";
        }
        if (lowerFaceEmotion.contains("smutn") && lowerQuizMood.contains("smutny/a")) {
            return "Wyraźnie smutny/a";
        }
        if (lowerFaceEmotion.contains("zł") && lowerQuizMood.contains("zły/a")) {
            return "Bardzo zły/a";
        }
        if (lowerFaceEmotion.contains("neutraln") && lowerQuizMood.contains("neutralny/a")) {
            return "Stabilny/a neutralny/a";
        }

        return quizMood + " (" + faceEmotion + ")";
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Zakończ ankietę")
                .setMessage("Czy na pewno chcesz zakończyć ankietę? Twoje odpowiedzi zostaną utracone.")
                .setPositiveButton("Tak", (dialog, which) -> resetQuestionnaire())
                .setNegativeButton("Nie", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static class SpotifyTrack {
        String uri;
        String trackName;

        SpotifyTrack(String uri, String trackName) {
            this.uri = uri;
            this.trackName = trackName;
        }
    }
}
