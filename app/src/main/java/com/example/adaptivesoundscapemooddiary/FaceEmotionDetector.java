package com.example.adaptivesoundscapemooddiary;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.util.List;

public class FaceEmotionDetector {

    public interface EmotionDetectionCallback {
        void onEmotionDetected(String emotion);

        void onFailure(String error);
    }

    private final FaceDetector detector;

    public FaceEmotionDetector() {

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        this.detector = FaceDetection.getClient(options);
    }

    public void detectEmotions(Bitmap bitmap, final EmotionDetectionCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (!faces.isEmpty()) {
                            Face face = faces.get(0);
                            String emotion = getDominantEmotion(face);
                            callback.onEmotionDetected(emotion);
                        } else {
                            callback.onFailure("Nie wykryto twarzy");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    private String getDominantEmotion(Face face) {
        // Dodaj sprawdzenie czy twarz jest prawidłowo wykryta
        if (face == null || face.getSmilingProbability() == null) {
            return null; // Zwracamy null gdy nie można określić emocji
        }

        float smileProb = face.getSmilingProbability();
        float leftEyeOpenProb = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0f;
        float rightEyeOpenProb = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0f;

        if (smileProb > 0.7f) {
            return "Radosna";
        } else if (smileProb < 0.3f && leftEyeOpenProb < 0.3f && rightEyeOpenProb < 0.3f) {
            return "Zmęczona";
        } else if (smileProb < 0.3f) {
            return "Smutna";
        }
        return "Neutralna";
    }
}
