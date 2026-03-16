package com.example.smart_attendance_system;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MarkAttendanceActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION = 101;
    private static final int CAMERA_PERMISSION   = 201;
    private static final int CAMERA_REQUEST      = 301;

    // ── Strict radius: 50 meters ──
    private static final float MAX_ALLOWED_RADIUS = 50f;

    // ── UI ──
    private TextView tvClassName, tvSubject, tvDate, tvLocation, tvClassInfo;
    private Button btnGetLocation, btnCaptureSelfie, btnMarkAttendance;

    // ── Firebase ──
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ── Location ──
    private FusedLocationProviderClient fusedLocationClient;
    private Location studentLocation;

    // ── Face ──
    private Bitmap capturedSelfie;
    private Bitmap profileBitmap;

    // ── Class Data ──
    private String classId;
    private double classLat    = 0;
    private double classLng    = 0;
    private double classRadius = 50; // default 50m

    // ── State flags ──
    private boolean locationVerified = false; // ✅ Step 1 passed
    private boolean selfieVerified   = false; // ✅ Step 2 passed

    // ── FaceNet ──
    private FaceNetHelper faceNetHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        // ── Bind Views ──
        tvClassName       = findViewById(R.id.tvClassName);
        tvSubject         = findViewById(R.id.tvSubject);
        tvDate            = findViewById(R.id.tvDate);
        tvLocation        = findViewById(R.id.tvLocation);
        tvClassInfo       = findViewById(R.id.tvClassInfo);
        btnGetLocation    = findViewById(R.id.btnGetLocation);
        btnCaptureSelfie  = findViewById(R.id.btnCaptureSelfie);
        btnMarkAttendance = findViewById(R.id.btnMarkAttendance);

        // ── Initial button states ──
        // Step 1: Get Location — always enabled
        // Step 2: Capture Selfie — LOCKED until location verified
        // Step 3: Mark Attendance — LOCKED until both steps done
        btnCaptureSelfie.setEnabled(false);
        btnMarkAttendance.setEnabled(false);

        // ── Firebase ──
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // ── Safety check ──
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── Class ID ──
        classId = getIntent().getStringExtra("classId");
        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Invalid class",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDate.setText(getTodayDate());

        // ── Load data ──
        loadClassDetails();
        loadProfilePhoto();

        // ── FaceNet init ──
        try {
            faceNetHelper = new FaceNetHelper(getAssets());
        } catch (Exception e) {
            Log.e("FACENET", "Init failed: " + e.getMessage());
        }

        // ── Button Listeners ──

        // STEP 1 — Get and verify location
        btnGetLocation.setOnClickListener(v -> requestLocation());

        // STEP 2 — Capture selfie (only unlocked after location verified)
        btnCaptureSelfie.setOnClickListener(v -> {
            if (!locationVerified) {
                Toast.makeText(this,
                        "Verify your location first (Step 1)",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            openCamera();
        });

        // STEP 3 — Mark attendance (only unlocked after both steps)
        btnMarkAttendance.setOnClickListener(v -> {
            if (!locationVerified) {
                Toast.makeText(this,
                        "Complete Step 1: Get Location",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!selfieVerified) {
                Toast.makeText(this,
                        "Complete Step 2: Capture Selfie",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            startFaceVerification();
        });
    }

    // ══════════════════════════════════════════
    //   STEP 1 — LOCATION FETCH & RADIUS CHECK
    // ══════════════════════════════════════════

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION);
            return;
        }

        tvLocation.setText("📍 Fetching your location...");
        btnGetLocation.setEnabled(false);

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    btnGetLocation.setEnabled(true);

                    if (loc == null) {
                        tvLocation.setText(
                                "❌ Could not get location.\nPlease enable GPS and try again.");
                        return;
                    }

                    studentLocation = loc;
                    checkIfInsideClassroom(loc);
                })
                .addOnFailureListener(e -> {
                    btnGetLocation.setEnabled(true);
                    tvLocation.setText("❌ Location error: " + e.getMessage());
                });
    }

    private void checkIfInsideClassroom(Location loc) {

        // Calculate distance between student and classroom
        float[] result = new float[1];
        Location.distanceBetween(
                classLat, classLng,
                loc.getLatitude(),
                loc.getLongitude(),
                result
        );

        float distance = result[0];
        float allowed  = Math.max((float) classRadius, MAX_ALLOWED_RADIUS);

        if (distance <= allowed) {
            // ✅ STEP 1 PASSED — Inside classroom
            locationVerified = true;

            tvLocation.setText(
                    "✅ Location Verified — Inside Classroom\n" +
                            "Distance: " + (int) distance + "m  " +
                            "| Allowed: " + (int) allowed + "m\n" +
                            "Accuracy: ±" + (int) loc.getAccuracy() + "m"
            );

            // ✅ Unlock Step 2
            btnCaptureSelfie.setEnabled(true);

            Toast.makeText(this,
                    "✅ Location verified! Now capture your selfie.",
                    Toast.LENGTH_SHORT).show();

        } else {
            // ❌ STEP 1 FAILED — Outside classroom
            locationVerified = false;
            capturedSelfie   = null;
            selfieVerified   = false;

            // Lock Step 2 and Step 3 again
            btnCaptureSelfie.setEnabled(false);
            btnMarkAttendance.setEnabled(false);

            tvLocation.setText(
                    "❌ Outside Classroom\n" +
                            "Your distance: " + (int) distance + "m\n" +
                            "Allowed radius: " + (int) allowed + "m\n" +
                            "You must be within " + (int) allowed + "m to mark attendance."
            );

            Toast.makeText(this,
                    "❌ You are " + (int) distance + "m away.\n" +
                            "Must be within " + (int) allowed + "m of classroom.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ══════════════════════════════════════════
    //   STEP 2 — CAMERA CAPTURE
    // ══════════════════════════════════════════

    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this,
                    "Camera not available",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);

        if (req == CAMERA_REQUEST && res == RESULT_OK
                && data != null && data.getExtras() != null) {

            capturedSelfie = (Bitmap) data.getExtras().get("data");
            selfieVerified = true;

            // ✅ Unlock Step 3
            btnMarkAttendance.setEnabled(true);

            Toast.makeText(this,
                    "✅ Selfie captured! Now mark your attendance.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ══════════════════════════════════════════
    //   STEP 3 — FACE VERIFICATION
    // ══════════════════════════════════════════

    private void startFaceVerification() {

        if (profileBitmap == null) {
            Toast.makeText(this,
                    "Profile photo still loading. Please wait.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnMarkAttendance.setEnabled(false);

        Toast.makeText(this,
                "🔍 Verifying face...",
                Toast.LENGTH_SHORT).show();

        compareFaces(profileBitmap, capturedSelfie);
    }

    private void compareFaces(Bitmap profile, Bitmap selfie) {

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(
                                FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        // Detect face in PROFILE photo
        detector.process(InputImage.fromBitmap(profile, 0))
                .addOnSuccessListener(profileFaces -> {

                    if (profileFaces.isEmpty()) {
                        btnMarkAttendance.setEnabled(true);
                        Toast.makeText(this,
                                "❌ No face found in profile photo.\n" +
                                        "Please update your profile photo.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Bitmap croppedProfile =
                            cropFace(profile, profileFaces.get(0));

                    // Detect face in SELFIE
                    detector.process(InputImage.fromBitmap(selfie, 0))
                            .addOnSuccessListener(selfieFaces -> {

                                if (selfieFaces.isEmpty()) {
                                    btnMarkAttendance.setEnabled(true);
                                    Toast.makeText(this,
                                            "❌ No face detected in selfie.\n" +
                                                    "Please retake in good lighting.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                Bitmap croppedSelfie =
                                        cropFace(selfie, selfieFaces.get(0));

                                // Both faces detected — run FaceNet
                                runFaceNet(croppedProfile, croppedSelfie);
                            })
                            .addOnFailureListener(e -> {
                                btnMarkAttendance.setEnabled(true);
                                Toast.makeText(this,
                                        "Selfie face detection failed",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnMarkAttendance.setEnabled(true);
                    Toast.makeText(this,
                            "Profile face detection failed",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void runFaceNet(Bitmap face1, Bitmap face2) {

        if (faceNetHelper == null) {
            // FaceNet model not loaded — skip face match, save attendance
            Log.w("FACENET", "FaceNet not available — skipping face match");
            saveAttendance();
            return;
        }

        Bitmap resized1 = Bitmap.createScaledBitmap(face1, 112, 112, true);
        Bitmap resized2 = Bitmap.createScaledBitmap(face2, 112, 112, true);

        float[] emb1 = faceNetHelper.getEmbedding(resized1);
        float[] emb2 = faceNetHelper.getEmbedding(resized2);

        float similarity = faceNetHelper.cosineSimilarity(emb1, emb2);
        Log.d("FACENET", "Similarity Score = " + similarity);

        if (similarity >= 0.75f) {
            // ✅ STEP 3 PASSED — Face matched
            Toast.makeText(this,
                    "✅ Face verified! Marking attendance...",
                    Toast.LENGTH_SHORT).show();
            saveAttendance();

        } else {
            // ❌ STEP 3 FAILED — Face mismatch
            btnMarkAttendance.setEnabled(true);
            selfieVerified = false;

            Toast.makeText(this,
                    "❌ Face does not match profile.\n" +
                            "Score: " + String.format(Locale.getDefault(),
                            "%.0f", similarity * 100) + "%\n" +
                            "Please retake your selfie.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap cropFace(Bitmap bitmap, Face face) {
        Rect bounds = face.getBoundingBox();
        int x      = Math.max(bounds.left, 0);
        int y      = Math.max(bounds.top, 0);
        int width  = Math.min(bounds.width(),  bitmap.getWidth()  - x);
        int height = Math.min(bounds.height(), bitmap.getHeight() - y);

        if (width <= 0 || height <= 0) return bitmap;
        return Bitmap.createBitmap(bitmap, x, y, width, height);
    }

    // ══════════════════════════════════════════
    //   FINAL — SAVE ATTENDANCE TO FIRESTORE
    // ══════════════════════════════════════════

    private void saveAttendance() {

        // Final safety checks before saving
        if (studentLocation == null || !locationVerified) {
            Toast.makeText(this,
                    "Location verification failed",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        float[] result = new float[1];
        Location.distanceBetween(
                classLat, classLng,
                studentLocation.getLatitude(),
                studentLocation.getLongitude(),
                result
        );

        Map<String, Object> map = new HashMap<>();
        map.put("studentId",  mAuth.getUid());
        map.put("classId",    classId);
        map.put("latitude",   studentLocation.getLatitude());
        map.put("longitude",  studentLocation.getLongitude());
        map.put("accuracy",   studentLocation.getAccuracy());
        map.put("distance",   result[0]);
        map.put("date",       getTodayDate());
        map.put("timestamp",  System.currentTimeMillis());
        map.put("verified",   true);

        db.collection("attendance")
                .add(map)
                .addOnSuccessListener(d -> {
                    Toast.makeText(this,
                            "✅ Attendance marked successfully!",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnMarkAttendance.setEnabled(true);
                    Toast.makeText(this,
                            "❌ Failed to save: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ══════════════════════════════════════════
    //            FIREBASE LOADERS
    // ══════════════════════════════════════════

    private void loadClassDetails() {
        db.collection("classes")
                .document(classId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this,
                                "Class not found",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String name    = doc.getString("className");
                    String subject = doc.getString("subject");
                    Double lat     = doc.getDouble("latitude");
                    Double lng     = doc.getDouble("longitude");
                    Double radius  = doc.getDouble("radius");

                    tvClassName.setText(name    != null ? name    : "—");
                    tvSubject.setText(subject   != null ? subject : "—");
                    tvClassInfo.setText(
                            (name    != null ? name    : "") +
                                    (subject != null ? " · " + subject : "")
                    );

                    if (lat    != null) classLat    = lat;
                    if (lng    != null) classLng    = lng;
                    if (radius != null) classRadius = radius;
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load class details",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void loadProfilePhoto() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this,
                                "Profile not found. Set up your profile first.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String base64 = doc.getString("profileImageBase64");

                    if (base64 == null || base64.isEmpty()) {
                        Toast.makeText(this,
                                "Please upload a profile photo first.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    profileBitmap = base64ToBitmap(base64);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load profile photo",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ══════════════════════════════════════════
    //               HELPERS
    // ══════════════════════════════════════════

    private Bitmap base64ToBitmap(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Invalid profile image",
                    Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("d MMMM yyyy",
                Locale.getDefault()).format(new Date());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        }

        if (requestCode == CAMERA_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }
}
