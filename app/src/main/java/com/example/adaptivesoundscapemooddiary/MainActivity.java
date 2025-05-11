package com.example.adaptivesoundscapemooddiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import android.content.Intent;
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

import com.github.dhaval2404.imagepicker.ImagePicker;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView emotionTextView, questionTextView, resultTextView;
    private FaceEmotionDetector faceEmotionDetector;
    private String detectedEmotion = "Nie wykryto";

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
            "Jak się dziś czujesz w skali 1-5?",
            "Jak oceniasz swój poziom stresu? (1-niski, 5-wysoki)",
            "Jak oceniasz jakość swojego snu?",
            "Czy masz dziś dużo energii?",
            "Jak oceniasz swoją produktywność?"
    };

    private static final Map<String, SpotifyTrack> MOOD_TO_SPOTIFY = new HashMap<String, SpotifyTrack>() {{
        put("szczęśliwy", new SpotifyTrack("spotify:track:5Q0Nhxo0l2bP3pNjpGJwV1", "Happy - Pharrell Williams"));
        put("smutny", new SpotifyTrack("spotify:track:6rqhFgbbKwnb9MLmUQDhG6", "Someone Like You - Adele"));
        put("zestresowany", new SpotifyTrack("spotify:track:1DkL3Z4Y6SUTgZYe9XUYQq", "Weightless - Marconi Union"));
        put("spokojny", new SpotifyTrack("spotify:track:3H6f6iIZWYg7mWbeJZ6v5b", "River Flows In You - Yiruma"));
        put("zmęczony", new SpotifyTrack("spotify:track:3KA0hL8hQZd9uzvo3JfEbX", "Starboy - The Weeknd"));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "mood-diary-db").build();

        // Inicjalizacja widoków
        faceEmotionDetector = new FaceEmotionDetector();
        questionTextView = findViewById(R.id.questionTextView);
        answerRadioGroup = findViewById(R.id.answerRadioGroup);
        nextButton = findViewById(R.id.nextButton);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        selfieImageView = findViewById(R.id.selfieImageView);
        questionnaireContainer = findViewById(R.id.questionnaireContainer);
        resultContainer = findViewById(R.id.resultContainer);
        resultTextView = findViewById(R.id.resultTextView);
        restartButton = findViewById(R.id.restartButton);
        viewHistoryButton = findViewById(R.id.viewHistoryButton);
        emotionTextView = findViewById(R.id.emotionTextView);
        startQuestionnaireButton = findViewById(R.id.startQuestionnaireButton);
        restartButton = findViewById(R.id.restartButton);
        questionnaireContainer = findViewById(R.id.questionnaireContainer);
        resultContainer = findViewById(R.id.resultContainer);

        lastEntryContainer = findViewById(R.id.lastEntryContainer);
        lastEntryImage = findViewById(R.id.lastEntryImage);
        lastEntryDate = findViewById(R.id.lastEntryDate);
        lastEntryMood = findViewById(R.id.lastEntryMood);
        lastEntrySong = findViewById(R.id.lastEntrySong);
        startQuestionnaireButton = findViewById(R.id.startQuestionnaireButton);

        questionnaireContainer.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);

        loadLastEntry();

        startQuestionnaireButton.setOnClickListener(v -> {
            questionnaireContainer.setVisibility(View.VISIBLE);
            startQuestionnaireButton.setVisibility(View.GONE); // ukryj przycisk po rozpoczęciu
            resultContainer.setVisibility(View.GONE);
        });

        showQuestion(currentQuestion);

        nextButton.setOnClickListener(v -> {
            int selectedId = answerRadioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Wybierz odpowiedź", Toast.LENGTH_SHORT).show();
                return;
            }

            int answerValue = 0;
            if (selectedId == R.id.option1) answerValue = 1;
            else if (selectedId == R.id.option2) answerValue = 2;
            else if (selectedId == R.id.option3) answerValue = 3;
            else if (selectedId == R.id.option4) answerValue = 4;
            else if (selectedId == R.id.option5) answerValue = 5;

            totalScore += answerValue;

            if (currentQuestion < questions.length - 1) {
                currentQuestion++;
                showQuestion(currentQuestion);
                answerRadioGroup.clearCheck();
            } else {
                finishQuestionnaire();
            }
        });

        takePhotoButton.setOnClickListener(v -> {
            ImagePicker.with(MainActivity.this)
                    .crop()
                    .compress(1024)
                    .maxResultSize(1080, 1080)
                    .start();
        });

        restartButton.setOnClickListener(v -> resetQuestionnaire());

        viewHistoryButton.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });
    }

    private void showQuestion(int questionIndex) {
        // Dodaj zabezpieczenie przed przekroczeniem zakresu
        if (questionIndex < 0 || questionIndex >= questions.length) {
            Toast.makeText(this, "Błąd: Nieprawidłowy indeks pytania", Toast.LENGTH_SHORT).show();
            return;
        }

        questionTextView.setText(questions[questionIndex]);

        RadioButton option1 = findViewById(R.id.option1);
        RadioButton option2 = findViewById(R.id.option2);
        RadioButton option3 = findViewById(R.id.option3);
        RadioButton option4 = findViewById(R.id.option4);
        RadioButton option5 = findViewById(R.id.option5);

        // Dodaj sprawdzenie nulli dla RadioButtons
        if (option1 != null && option2 != null && option3 != null && option4 != null && option5 != null) {
            if (questionIndex == 1) {
                option1.setText("1 - Niski");
                option5.setText("5 - Wysoki");
            } else {
                option1.setText("1");
                option5.setText("5");
            }
        }

        nextButton.setText(questionIndex == questions.length - 1 ? "Zakończ ankietę" : "Następne pytanie");
    }

    private void finishQuestionnaire() {
        if (questions.length == 0 || totalScore < 0) {
            Toast.makeText(this, "Błąd obliczania wyniku", Toast.LENGTH_SHORT).show();
            return;
        }
        String detectedMood = analyzeMood(totalScore);
        String finalMood = combineMoods(detectedMood, detectedEmotion);

        SpotifyTrack spotifyTrack = MOOD_TO_SPOTIFY.getOrDefault(detectedMood.toLowerCase(),
                new SpotifyTrack("spotify:track:0Dg5BZ2tkBUkUYIMWbc4wG", "Default Track"));

        MoodEntry newEntry = new MoodEntry(
                new Date(),
                "Wynik: " + totalScore + "/25",
                finalMood,
                selfieUri,
                spotifyTrack.uri,
                spotifyTrack.trackName
        );

        new Thread(() -> {
            db.moodEntryDao().insert(newEntry);

            runOnUiThread(() -> {
                // Ukryj komunikat o emocji
                emotionTextView.setVisibility(View.GONE);
                emotionTextView.setText("");

                showResult(detectedMood, totalScore);
                loadLastEntry();
                lastEntryContainer.setVisibility(View.VISIBLE);

                // Ukryj "Rozpocznij nową ankietę" — zostaw tylko "Wypełnij ponownie"
                startQuestionnaireButton.setVisibility(View.GONE);

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyTrack.uri)));
                } catch (Exception e) {
                    Toast.makeText(this, "Błąd przy otwieraniu Spotify", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showResult(String mood, int score) {
        selfieImageView.setVisibility(View.GONE);
        questionnaireContainer.setVisibility(View.GONE);
        resultContainer.setVisibility(View.VISIBLE);
        resultTextView.setText("Twój nastrój: " + mood + "\nWynik: " + score + "/25");
    }

    private void resetQuestionnaire() {
        currentQuestion = 0;
        totalScore = 0;
        selfieUri = null;
        selfieImageView.setVisibility(View.GONE);
        detectedEmotion = "Nie wykryto";

        if (emotionTextView != null) {
            emotionTextView.setVisibility(View.GONE);
            emotionTextView.setText("");
        }

        resultContainer.setVisibility(View.GONE);
        questionnaireContainer.setVisibility(View.VISIBLE);
        showQuestion(currentQuestion);
        answerRadioGroup.clearCheck();

        lastEntryContainer.setVisibility(View.GONE);
        startQuestionnaireButton.setVisibility(View.GONE);
    }



    private String analyzeMood(int score) {
        if (score >= 20) return "szczęśliwy";
        else if (score >= 15) return "spokojny";
        else if (score >= 10) return "neutralny";
        else if (score >= 5) return "smutny";
        else return "zestresowany";
    }

    private void loadLastEntry() {
        new Thread(() -> {
            MoodEntry lastEntry = db.moodEntryDao().getLastEntry();
            runOnUiThread(() -> {
                if (lastEntry != null) {
                    String dateText = lastEntry.getDate() != null ?
                            DateFormat.getDateInstance().format(lastEntry.getDate()) : "Brak daty";
                    lastEntryDate.setText("Data: " + dateText);
                    lastEntryMood.setText("Nastrój: " + lastEntry.getDetectedMood());  // lub getMoodText(), zależnie co chcesz
                    lastEntrySong.setText("Utwór: " + lastEntry.getSpotifyTrackName());

                    if (lastEntry.getSelfieUri() != null) {
                        lastEntryImage.setImageURI(Uri.parse(lastEntry.getSelfieUri()));
                        lastEntryImage.setVisibility(View.VISIBLE);
                    } else {
                        lastEntryImage.setVisibility(View.GONE);
                    }
                    lastEntryContainer.setVisibility(View.VISIBLE);
                } else {
                    lastEntryContainer.setVisibility(View.GONE);
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
        // Zabezpieczenie przed null/empty dla quizMood
        if (quizMood == null || quizMood.isEmpty()) {
            return faceEmotion != null ? faceEmotion : "Nie określono";
        }

        if (faceEmotion == null || faceEmotion.isEmpty() || faceEmotion.equals("Nie wykryto")) {
            return quizMood;
        }

        // Normalizacja Stringów
        String lowerFaceEmotion = faceEmotion.toLowerCase().trim();
        String lowerQuizMood = quizMood.toLowerCase().trim();

        if (lowerFaceEmotion.contains("radosn") && lowerQuizMood.contains("szczęśliwy")) {
            return "Bardzo szczęśliwy";
        }
        if (lowerFaceEmotion.contains("smutn") && lowerQuizMood.contains("smutny")) {
            return "Wyraźnie smutny";
        }

        return quizMood + " (" + faceEmotion + ")";
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
