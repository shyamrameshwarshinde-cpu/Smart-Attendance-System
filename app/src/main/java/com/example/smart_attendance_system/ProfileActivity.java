package com.example.smart_attendance_system;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private EditText etName, etEmail, etEnrollment;
    private Button btnSaveProfile, btnEditProfile;
    private TextView btnUploadPhoto; // ✅ FIXED: TextView not Button

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String profileImageBase64 = null;

    // ── Pick image from gallery ──
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Bitmap bitmap = uriToBitmap(uri);
                    if (bitmap == null) return;

                    ivProfile.setImageBitmap(bitmap);
                    profileImageBase64 = bitmapToBase64(bitmap);
                    btnSaveProfile.setEnabled(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ── Bind Views ──
        ivProfile       = findViewById(R.id.ivProfile);
        etName          = findViewById(R.id.etName);
        etEmail         = findViewById(R.id.etEmail);
        etEnrollment    = findViewById(R.id.etEnrollment);
        btnUploadPhoto  = findViewById(R.id.btnUploadPhoto); // ✅ TextView
        btnSaveProfile  = findViewById(R.id.btnSaveProfile);
        btnEditProfile  = findViewById(R.id.btnEditProfile);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // ── Safety check ──
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── Initial state — view mode ──
        setViewMode();

        // ── Set email (read-only, never editable) ──
        etEmail.setText(mAuth.getCurrentUser().getEmail());

        // ── Load existing profile from Firestore ──
        loadProfile();

        // ── Button Listeners ──
        btnEditProfile.setOnClickListener(v -> enableEditing());

        btnUploadPhoto.setOnClickListener(v ->
                pickImageLauncher.launch("image/*")
        );

        btnSaveProfile.setOnClickListener(v -> {
            if (profileImageBase64 == null) {
                // ✅ Allow saving without new photo
                // if they already have a saved photo
                saveProfile();
                return;
            }
            Bitmap bitmap = base64ToBitmap(profileImageBase64);
            validateFaceAndSave(bitmap);
        });
    }

    // ── View mode — all fields read-only ──
    private void setViewMode() {
        etName.setEnabled(false);
        etEnrollment.setEnabled(false);
        etEmail.setEnabled(false);
        btnUploadPhoto.setEnabled(false);
        btnSaveProfile.setEnabled(false);
        btnEditProfile.setEnabled(true);
    }

    // ── Edit mode — fields editable ──
    private void enableEditing() {
        etName.setEnabled(true);
        etEnrollment.setEnabled(true);
        btnUploadPhoto.setEnabled(true);
        btnSaveProfile.setEnabled(true);
        Toast.makeText(this,
                "Edit mode enabled",
                Toast.LENGTH_SHORT).show();
    }

    // ── ML Kit — validate exactly one face before saving ──
    private void validateFaceAndSave(Bitmap bitmap) {

        btnSaveProfile.setEnabled(false);

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(
                                FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(faces -> {

                    if (faces.size() == 1) {
                        saveProfile(); // ✅ Exactly one face
                    } else if (faces.isEmpty()) {
                        btnSaveProfile.setEnabled(true);
                        Toast.makeText(this,
                                "No face detected. Use a clear front face photo.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        btnSaveProfile.setEnabled(true);
                        Toast.makeText(this,
                                "Multiple faces found. Use a photo with ONE face only.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this,
                            "Face detection failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── Save profile to Firestore ──
    private void saveProfile() {

        String name       = etName.getText().toString().trim();
        String enrollment = etEnrollment.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this,
                    "Enter your name",
                    Toast.LENGTH_SHORT).show();
            btnSaveProfile.setEnabled(true);
            return;
        }

        if (enrollment.isEmpty()) {
            Toast.makeText(this,
                    "Enter your enrollment number",
                    Toast.LENGTH_SHORT).show();
            btnSaveProfile.setEnabled(true);
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> map = new HashMap<>();
        map.put("name",       name);
        map.put("enrollment", enrollment);

        // Only update image if a new one was picked
        if (profileImageBase64 != null) {
            map.put("profileImageBase64", profileImageBase64);
        }

        // ✅ Save to both "users" and "students" collections
        db.collection("users")
                .document(uid)
                .set(map, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        db.collection("students")
                                .document(uid)
                                .set(map, SetOptions.merge())
                                .addOnSuccessListener(unused2 -> {
                                    Toast.makeText(this,
                                            "Profile saved successfully ✅",
                                            Toast.LENGTH_SHORT).show();
                                    setViewMode();
                                })
                                .addOnFailureListener(e -> {
                                    btnSaveProfile.setEnabled(true);
                                    Toast.makeText(this,
                                            "Save error: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                })
                )
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this,
                            "Save error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── Load profile from Firestore ──
    private void loadProfile() {

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc == null || !doc.exists()) return;

                    String name = doc.getString("name");
                    if (name != null) etName.setText(name);

                    String enrollment = doc.getString("enrollment");
                    if (enrollment != null) etEnrollment.setText(enrollment);

                    String imageBase64 = doc.getString("profileImageBase64");
                    if (imageBase64 != null && !imageBase64.isEmpty()) {
                        profileImageBase64 = imageBase64;
                        ivProfile.setImageBitmap(base64ToBitmap(imageBase64));
                    }

                    setViewMode();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load profile",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ── URI → Bitmap with EXIF rotation fix ──
    private Bitmap uriToBitmap(Uri uri) {
        try {
            InputStream imageStream =
                    getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            if (imageStream != null) imageStream.close();
            if (bitmap == null) return null;

            // Read EXIF and fix rotation
            InputStream exifStream =
                    getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(exifStream);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            if (exifStream != null) exifStream.close();

            bitmap = applyExifRotation(bitmap, orientation);

            // Resize to max 640px for ML Kit
            int max = 640;
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            if (w > max || h > max) {
                float ratio = Math.min((float) max / w, (float) max / h);
                bitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        Math.round(w * ratio),
                        Math.round(h * ratio),
                        true
                );
            }

            return bitmap;

        } catch (Exception e) {
            Toast.makeText(this,
                    "Image error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ── Apply EXIF rotation — all 6 orientations ──
    private Bitmap applyExifRotation(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1.0f, -1.0f);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return bitmap;
        }

        return Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix, true
        );
    }

    // ── Bitmap ↔ Base64 ──
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}