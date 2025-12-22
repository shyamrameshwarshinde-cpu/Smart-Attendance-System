//package com.example.smart_attendance_system;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Bundle;
//import android.util.Base64;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.SetOptions;
//import com.google.mlkit.vision.common.InputImage;
//import com.google.mlkit.vision.face.FaceDetection;
//import com.google.mlkit.vision.face.FaceDetector;
//import com.google.mlkit.vision.face.FaceDetectorOptions;
//
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
//
//public class ProfileActivity extends AppCompatActivity {
//
//    private ImageView ivProfile;
//    private EditText etName, etEmail;
//    private Button btnUploadPhoto, btnSaveProfile, btnEditProfile;
//
//    private FirebaseAuth mAuth;
//    private FirebaseFirestore db;
//
//    private String profileImageBase64 = null;
//    private boolean isEditMode = false;
//
//    // 📷 Pick image from gallery
//    private final ActivityResultLauncher<String> pickImageLauncher =
//            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
//                if (uri != null) {
//                    Bitmap bitmap = uriToBitmap(uri);
//                    if (bitmap == null) return;
//
//                    ivProfile.setImageBitmap(bitmap);
//                    profileImageBase64 = bitmapToBase64(bitmap);
//                    btnSaveProfile.setEnabled(true);
//                }
//            });
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_profile);
//
//        ivProfile = findViewById(R.id.ivProfile);
//        etName = findViewById(R.id.etName);
//        etEmail = findViewById(R.id.etEmail);
//        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
//        btnSaveProfile = findViewById(R.id.btnSaveProfile);
//        btnEditProfile = findViewById(R.id.btnEditProfile);
//
//        mAuth = FirebaseAuth.getInstance();
//        db = FirebaseFirestore.getInstance();
//
//        etEmail.setEnabled(false);
//        btnSaveProfile.setEnabled(false);
//
//        if (mAuth.getCurrentUser() != null) {
//            etEmail.setText(mAuth.getCurrentUser().getEmail());
//        }
//
//        loadProfile();
//
//        btnEditProfile.setOnClickListener(v -> enableEditing());
//
//        btnUploadPhoto.setOnClickListener(v ->
//                pickImageLauncher.launch("image/*")
//        );
//
//        btnSaveProfile.setOnClickListener(v -> {
//            if (profileImageBase64 == null) {
//                Toast.makeText(this, "Upload clear face photo", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            Bitmap bitmap = base64ToBitmap(profileImageBase64);
//            validateFaceAndSave(bitmap);
//        });
//    }
//
//    // 🔓 Enable edit mode
//    private void enableEditing() {
//        isEditMode = true;
//        etName.setEnabled(true);
//        btnUploadPhoto.setEnabled(true);
//        btnSaveProfile.setEnabled(true);
//    }
//
//    // 🔥 Validate face using ML Kit BEFORE saving
//    private void validateFaceAndSave(Bitmap bitmap) {
//
//        FaceDetectorOptions options =
//                new FaceDetectorOptions.Builder()
//                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//                        .build();
//
//        FaceDetector detector = FaceDetection.getClient(options);
//
//        detector.process(InputImage.fromBitmap(bitmap, 0))
//                .addOnSuccessListener(faces -> {
//                    if (faces.size() == 1) {
//                        saveProfile(); // ✅ exactly one face
//                    } else {
//                        Toast.makeText(
//                                this,
//                                "Use a clear photo with ONE front face",
//                                Toast.LENGTH_LONG
//                        ).show();
//                    }
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(this,
//                                "Face detection failed",
//                                Toast.LENGTH_SHORT).show()
//                );
//    }
//
//    // 💾 Save profile to Firestore
//    private void saveProfile() {
//
//        String name = etName.getText().toString().trim();
//        if (name.isEmpty()) {
//            Toast.makeText(this, "Enter name", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String uid = mAuth.getCurrentUser().getUid();
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("name", name);
//        map.put("profileImageBase64", profileImageBase64);
//
//        db.collection("students")
//                .document(uid)
//                .set(map, SetOptions.merge())
//                .addOnSuccessListener(unused -> {
//                    Toast.makeText(this,
//                            "Profile saved successfully ✅",
//                            Toast.LENGTH_SHORT).show();
//                    finish();
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(this,
//                                e.getMessage(),
//                                Toast.LENGTH_SHORT).show()
//                );
//    }
//
//    // 📥 Load profile
//    private void loadProfile() {
//
//        String uid = mAuth.getCurrentUser().getUid();
//
//        db.collection("students")
//                .document(uid)
//                .get()
//                .addOnSuccessListener(doc -> {
//                    if (!doc.exists()) return;
//
//                    etName.setText(doc.getString("name"));
//
//                    if (doc.contains("profileImageBase64")) {
//                        profileImageBase64 = doc.getString("profileImageBase64");
//                        ivProfile.setImageBitmap(base64ToBitmap(profileImageBase64));
//                    }
//
//                    etName.setEnabled(false);
//                    btnUploadPhoto.setEnabled(false);
//                    btnSaveProfile.setEnabled(false);
//                    isEditMode = false;
//                });
//    }
//
//    // 🔁 Helpers
//
//    private Bitmap uriToBitmap(Uri uri) {
//        try {
//            InputStream is = getContentResolver().openInputStream(uri);
//            Bitmap bitmap = BitmapFactory.decodeStream(is);
//
//            // Keep aspect ratio – resize only if large
//            int max = 640;
//            int w = bitmap.getWidth();
//            int h = bitmap.getHeight();
//
//            if (w > max || h > max) {
//                float ratio = Math.min((float) max / w, (float) max / h);
//                bitmap = Bitmap.createScaledBitmap(
//                        bitmap,
//                        Math.round(w * ratio),
//                        Math.round(h * ratio),
//                        true
//                );
//            }
//            return bitmap;
//        } catch (Exception e) {
//            Toast.makeText(this, "Image error", Toast.LENGTH_SHORT).show();
//            return null;
//        }
//    }
//
//    private String bitmapToBase64(Bitmap bitmap) {
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
//        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
//    }
//
//    private Bitmap base64ToBitmap(String base64) {
//        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
//        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//    }
//}
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
    private EditText etName, etEmail;
    private Button btnUploadPhoto, btnSaveProfile, btnEditProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String profileImageBase64 = null;
    private boolean isEditMode = false;

    // 📷 Pick image from gallery
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

        ivProfile = findViewById(R.id.ivProfile);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail.setEnabled(false);
        btnSaveProfile.setEnabled(false);

        if (mAuth.getCurrentUser() != null) {
            etEmail.setText(mAuth.getCurrentUser().getEmail());
        }

        loadProfile();

        btnEditProfile.setOnClickListener(v -> enableEditing());

        btnUploadPhoto.setOnClickListener(v ->
                pickImageLauncher.launch("image/*")
        );

        btnSaveProfile.setOnClickListener(v -> {
            if (profileImageBase64 == null) {
                Toast.makeText(this,
                        "Upload a clear front face photo",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap bitmap = base64ToBitmap(profileImageBase64);
            validateFaceAndSave(bitmap);
        });
    }

    // 🔓 Enable edit mode
    private void enableEditing() {
        isEditMode = true;
        etName.setEnabled(true);
        btnUploadPhoto.setEnabled(true);
        btnSaveProfile.setEnabled(true);
    }

    // 🧠 ML Kit face validation before saving
    private void validateFaceAndSave(Bitmap bitmap) {

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 1) {
                        saveProfile(); // ✅ exactly one face
                    } else {
                        Toast.makeText(this,
                                "Use ONE clear front face photo",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Face detection failed",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // 💾 Save profile to Firestore
    private void saveProfile() {

        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this,
                    "Enter name",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("profileImageBase64", profileImageBase64);

        db.collection("students")
                .document(uid)
                .set(map, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Profile saved successfully ✅",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    // 📥 Load existing profile
    private void loadProfile() {

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    etName.setText(doc.getString("name"));

                    if (doc.contains("profileImageBase64")) {
                        profileImageBase64 = doc.getString("profileImageBase64");
                        ivProfile.setImageBitmap(
                                base64ToBitmap(profileImageBase64)
                        );
                    }

                    etName.setEnabled(false);
                    btnUploadPhoto.setEnabled(false);
                    btnSaveProfile.setEnabled(false);
                    isEditMode = false;
                });
    }

    // 🔁 Helpers

    private Bitmap uriToBitmap(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            // 🔄 FIX ROTATION (MOST IMPORTANT FIX)
            bitmap = fixRotation(bitmap);

            // Resize for ML Kit
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
                    "Image error",
                    Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // 🔄 Fix horizontal / rotated image
    private Bitmap fixRotation(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90); // works for most phones
        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix, true);
    }

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
