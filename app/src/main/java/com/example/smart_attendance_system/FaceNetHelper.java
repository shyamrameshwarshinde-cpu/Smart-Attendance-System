package com.example.smart_attendance_system;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class FaceNetHelper {

    private Interpreter interpreter;
    private final int INPUT_SIZE = 160;
    private final int EMBEDDING_SIZE = 128;

    public FaceNetHelper(AssetManager assetManager) throws IOException {
        interpreter = new Interpreter(loadModel(assetManager));
    }

    private ByteBuffer loadModel(AssetManager assetManager) throws IOException {
        AssetFileDescriptor fileDescriptor =
                assetManager.openFd("facenet.tflite");
        FileInputStream inputStream =
                new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength);
    }

    public float[] getEmbedding(Bitmap bitmap) {

        bitmap = Bitmap.createScaledBitmap(bitmap,
                INPUT_SIZE, INPUT_SIZE, true);

        ByteBuffer inputBuffer =
                ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);

        inputBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0,
                INPUT_SIZE, 0, 0,
                INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255f);
            inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255f);
            inputBuffer.putFloat((pixel & 0xFF) / 255f);
        }

        float[][] output = new float[1][EMBEDDING_SIZE];

        interpreter.run(inputBuffer, output);

        return output[0];
    }

    public float cosineSimilarity(float[] emb1, float[] emb2) {

        float dot = 0f;
        float norm1 = 0f;
        float norm2 = 0f;

        for (int i = 0; i < emb1.length; i++) {
            dot += emb1[i] * emb2[i];
            norm1 += emb1[i] * emb1[i];
            norm2 += emb2[i] * emb2[i];
        }

        return dot / ((float)
                (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }
}
