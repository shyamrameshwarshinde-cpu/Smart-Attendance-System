//package com.example.smart_attendance_system;
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.location.Location;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Base64;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.location.Priority;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.mlkit.vision.common.InputImage;
//import com.google.mlkit.vision.face.FaceDetection;
//import com.google.mlkit.vision.face.FaceDetector;
//import com.google.mlkit.vision.face.FaceDetectorOptions;
//
//import java.io.ByteArrayOutputStream;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
//public class MarkAttendanceActivity extends AppCompatActivity {
//
//    // 🔐 Permission codes
//    private static final int LOCATION_PERMISSION_CODE = 101;
//    private static final int CAMERA_PERMISSION_CODE = 201;
//    private static final int CAMERA_REQUEST_CODE = 301;
//
//    // UI
//    private TextView tvClassName, tvSubject, tvDate, tvLocation;
//    private Button btnGetLocation, btnCaptureSelfie, btnMarkAttendance;
//
//    // Firebase
//    private FirebaseAuth mAuth;
//    private FirebaseFirestore db;
//
//    // Location
//    private FusedLocationProviderClient fusedLocationClient;
//    private LocationCallback locationCallback;
//    private Location studentLocation;
//
//    // Camera
//    private Bitmap capturedSelfie;
//
//    // Class data
//    private String classId;
//    private double classLat;
//    private double classLng;
//    private double classRadius = 10; // meters
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_mark_attendance);
//
//        // Bind UI
//        tvClassName = findViewById(R.id.tvClassName);
//        tvSubject = findViewById(R.id.tvSubject);
//        tvDate = findViewById(R.id.tvDate);
//        tvLocation = findViewById(R.id.tvLocation);
//
//        btnGetLocation = findViewById(R.id.btnGetLocation);
//        btnCaptureSelfie = findViewById(R.id.btnCaptureSelfie);
//        btnMarkAttendance = findViewById(R.id.btnMarkAttendance);
//
//        btnMarkAttendance.setEnabled(false);
//
//        // Init Firebase & Location
//        mAuth = FirebaseAuth.getInstance();
//        db = FirebaseFirestore.getInstance();
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//
//        // Get classId
//        classId = getIntent().getStringExtra("classId");
//        if (classId == null) {
//            Toast.makeText(this, "Invalid class", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        loadClassDetails();
//
//        btnGetLocation.setOnClickListener(v -> checkLocationPermission());
//        btnCaptureSelfie.setOnClickListener(v -> openCamera());
//
//        btnMarkAttendance.setOnClickListener(v -> {
//            if (studentLocation == null) {
//                Toast.makeText(this, "Get location first", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (capturedSelfie == null) {
//                Toast.makeText(this, "Capture live selfie first", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            validateAndSaveAttendance();
//        });
//    }
//
//    private void fetchProfileAndCompareFace(float distance) {
//
//        String studentId = mAuth.getCurrentUser().getUid();
//
//        db.collection("students")
//                .document(studentId)
//                .get()
//                .addOnSuccessListener(doc -> {
//
//                    if (!doc.exists()) {
//                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    String profileBase64 = doc.getString("profileImageBase64");
//
//                    if (profileBase64 == null || profileBase64.isEmpty()) {
//                        Toast.makeText(this, "Profile photo missing", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    Bitmap profileBitmap = base64ToBitmap(profileBase64);
//                    compareFaces(profileBitmap, capturedSelfie, distance);
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
//                );
//    }
//
//    private Bitmap base64ToBitmap(String base64) {
//        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
//        return android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
//    }
//
//    private void compareFaces(Bitmap profile, Bitmap selfie, float distance) {
//
//        InputImage img1 = InputImage.fromBitmap(profile, 0);
//        InputImage img2 = InputImage.fromBitmap(selfie, 0);
//
//        FaceDetectorOptions options =
//                new FaceDetectorOptions.Builder()
//                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//                        .build();
//
//        FaceDetector detector = FaceDetection.getClient(options);
//
//        detector.process(img1)
//                .addOnSuccessListener(profileFaces -> {
//
//                    if (profileFaces.isEmpty()) {
//                        Toast.makeText(this, "No face in profile photo", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    detector.process(img2)
//                            .addOnSuccessListener(selfieFaces -> {
//
//                                if (selfieFaces.isEmpty()) {
//                                    Toast.makeText(this, "No face in selfie", Toast.LENGTH_SHORT).show();
//                                    return;
//                                }
//
//                                // ✅ BASIC MATCH SUCCESS
//                                saveAttendance(distance);
//
//                            })
//                            .addOnFailureListener(e ->
//                                    Toast.makeText(this, "Selfie face detection failed", Toast.LENGTH_SHORT).show()
//                            );
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(this, "Profile face detection failed", Toast.LENGTH_SHORT).show()
//                );
//    }
//
//
//
//    // 🔹 Load class details
//    private void loadClassDetails() {
//        db.collection("classes")
//                .document(classId)
//                .get()
//                .addOnSuccessListener(doc -> {
//                    if (!doc.exists()) {
//                        Toast.makeText(this, "Class not found", Toast.LENGTH_SHORT).show();
//                        finish();
//                        return;
//                    }
//
//                    tvClassName.setText(doc.getString("className"));
//                    tvSubject.setText(doc.getString("subject"));
//                    tvDate.setText(doc.getString("date"));
//
//                    Double lat = doc.getDouble("latitude");
//                    Double lng = doc.getDouble("longitude");
//                    Double radius = doc.getDouble("radius");
//
//                    if (lat == null || lng == null) {
//                        Toast.makeText(this, "Class location missing", Toast.LENGTH_SHORT).show();
//                        finish();
//                        return;
//                    }
//
//                    classLat = lat;
//                    classLng = lng;
//                    if (radius != null) classRadius = radius;
//                });
//    }
//
//    // 🔐 Location permission
//    private void checkLocationPermission() {
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(
//                    this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    LOCATION_PERMISSION_CODE
//            );
//        } else {
//            requestAccurateLocation();
//        }
//    }
//
//    // 📍 Get accurate GPS
//    private void requestAccurateLocation() {
//
//        tvLocation.setText("Getting accurate location...");
//        btnMarkAttendance.setEnabled(false);
//
//        LocationRequest request = new LocationRequest.Builder(
//                Priority.PRIORITY_HIGH_ACCURACY,
//                2000
//        ).setWaitForAccurateLocation(true)
//                .setMinUpdateDistanceMeters(1)
//                .build();
//
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult result) {
//                Location loc = result.getLastLocation();
//                if (loc == null) return;
//
//                if (loc.getAccuracy() > 20) {
//                    tvLocation.setText("Waiting for better GPS...\nAccuracy: "
//                            + (int) loc.getAccuracy() + "m");
//                    return;
//                }
//
//                studentLocation = loc;
//
//                tvLocation.setText(
//                        "Lat: " + loc.getLatitude() +
//                                "\nLng: " + loc.getLongitude() +
//                                "\nAccuracy: " + (int) loc.getAccuracy() + "m"
//                );
//
//                btnMarkAttendance.setEnabled(true);
//                fusedLocationClient.removeLocationUpdates(locationCallback);
//            }
//        };
//
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
//
//        fusedLocationClient.requestLocationUpdates(
//                request,
//                locationCallback,
//                getMainLooper()
//        );
//    }
//
//    // 📸 Camera open with permission
//    private void openCamera() {
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(
//                    this,
//                    new String[]{Manifest.permission.CAMERA},
//                    CAMERA_PERMISSION_CODE
//            );
//            return;
//        }
//
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (intent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(intent, CAMERA_REQUEST_CODE);
//        } else {
//            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    // 📏 Distance check
//    private void validateAndSaveAttendance() {
//
//        float[] results = new float[1];
//        Location.distanceBetween(
//                classLat, classLng,
//                studentLocation.getLatitude(),
//                studentLocation.getLongitude(),
//                results
//        );
//
//        float distance = results[0];
//        float allowed = (float) classRadius + studentLocation.getAccuracy();
//
//        if (distance <= allowed) {
//
//            // 🔥 CHANGE IS HERE
//            fetchProfileAndCompareFace(distance);
//
//        } else {
//            Toast.makeText(
//                    this,
//                    "Outside classroom\nDistance: " + (int) distance +
//                            "m\nAllowed: " + (int) allowed + "m",
//                    Toast.LENGTH_LONG
//            ).show();
//        }
//    }
//
//    // ✅ Save attendance
//    private void saveAttendance(float distance) {
//
//        String studentId = mAuth.getCurrentUser().getUid();
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("studentId", studentId);
//        data.put("classId", classId);
//        data.put("latitude", studentLocation.getLatitude());
//        data.put("longitude", studentLocation.getLongitude());
//        data.put("accuracy", studentLocation.getAccuracy());
//        data.put("distance", distance);
//        data.put("selfie", bitmapToBase64(capturedSelfie));
//        data.put("selfieTaken", true);
//        data.put("date", getTodayDate());
//        data.put("timestamp", System.currentTimeMillis());
//
//        db.collection("attendance")
//                .add(data)
//                .addOnSuccessListener(d -> {
//                    Toast.makeText(this, "Attendance marked ✅", Toast.LENGTH_SHORT).show();
//                    finish();
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
//    }
//
//    // 🔁 Permission result
//    @Override
//    public void onRequestPermissionsResult(
//            int requestCode,
//            @NonNull String[] permissions,
//            @NonNull int[] grantResults) {
//
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == LOCATION_PERMISSION_CODE &&
//                grantResults.length > 0 &&
//                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            requestAccurateLocation();
//        }
//
//        if (requestCode == CAMERA_PERMISSION_CODE &&
//                grantResults.length > 0 &&
//                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            openCamera();
//        }
//    }
//
//    // 📸 Camera result
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
//            if (data != null && data.getExtras() != null) {
//                capturedSelfie = (Bitmap) data.getExtras().get("data");
//                Toast.makeText(this, "Selfie captured ✅", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    private String bitmapToBase64(Bitmap bitmap) {
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
//        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
//    }
//
//    private String getTodayDate() {
//        return new SimpleDateFormat("yyyy-MM-dd",
//                Locale.getDefault()).format(new Date());
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (locationCallback != null) {
//            fusedLocationClient.removeLocationUpdates(locationCallback);
//        }
//    }
//}
package com.example.smart_attendance_system;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class MarkAttendanceActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION = 101;
    private static final int CAMERA_PERMISSION = 201;
    private static final int CAMERA_REQUEST = 301;

    private TextView tvClassName, tvDate, tvLocation;
    private Button btnGetLocation, btnCaptureSelfie, btnMarkAttendance;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private Location studentLocation;
    private Bitmap capturedSelfie;
    private Bitmap profileBitmap;

    private String classId;
    private double classLat, classLng, classRadius = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        tvClassName = findViewById(R.id.tvClassName);
        tvDate = findViewById(R.id.tvDate);
        tvLocation = findViewById(R.id.tvLocation);

        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnCaptureSelfie = findViewById(R.id.btnCaptureSelfie);
        btnMarkAttendance = findViewById(R.id.btnMarkAttendance);

        btnMarkAttendance.setEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        classId = getIntent().getStringExtra("classId");
        tvDate.setText(getTodayDate());

        loadClassDetails();
        loadProfilePhoto();

        btnGetLocation.setOnClickListener(v -> requestLocation());
        btnCaptureSelfie.setOnClickListener(v -> openCamera());
        btnMarkAttendance.setOnClickListener(v -> validateDistance());
    }

    // ================= LOCATION =================
    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc == null) return;
                    studentLocation = loc;
                    tvLocation.setText("Location OK (" + (int) loc.getAccuracy() + "m)");
                    btnMarkAttendance.setEnabled(true);
                });
    }

    private void validateDistance() {
        if (studentLocation == null || capturedSelfie == null) {
            Toast.makeText(this, "Capture selfie & location first", Toast.LENGTH_SHORT).show();
            return;
        }

        float[] res = new float[1];
        Location.distanceBetween(
                classLat, classLng,
                studentLocation.getLatitude(),
                studentLocation.getLongitude(),
                res);

        if (res[0] > classRadius + studentLocation.getAccuracy()) {
            Toast.makeText(this, "Outside classroom ❌", Toast.LENGTH_LONG).show();
            return;
        }

        compareFaces(profileBitmap, capturedSelfie);
    }

    // ================= CAMERA =================
    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION);
            return;
        }

        startActivityForResult(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == CAMERA_REQUEST && res == RESULT_OK) {
            capturedSelfie = (Bitmap) data.getExtras().get("data");
            Toast.makeText(this, "Selfie captured", Toast.LENGTH_SHORT).show();
        }
    }

    // ================= FACE RECOGNITION =================
    private void compareFaces(Bitmap profile, Bitmap selfie) {

        FaceDetector detector = FaceDetection.getClient(
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .build());

        detector.process(InputImage.fromBitmap(profile, 0))
                .addOnSuccessListener(pFaces -> {
                    if (pFaces.size() != 1) {
                        Toast.makeText(this, "Invalid profile photo", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Bitmap pFace = normalizeFace(cropFace(profile, pFaces.get(0)));

                    detector.process(InputImage.fromBitmap(selfie, 0))
                            .addOnSuccessListener(sFaces -> {
                                if (sFaces.size() != 1) {
                                    Toast.makeText(this, "Ensure one clear face", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Bitmap sFace = normalizeFace(cropFace(selfie, sFaces.get(0)));

                                float similarity = calculateSimilarity(pFace, sFace);

                                if (similarity >= 0.80f) {
                                    saveAttendance();
                                } else {
                                    Toast.makeText(this, "Face mismatch ❌", Toast.LENGTH_LONG).show();
                                }
                            });
                });
    }

    private Bitmap cropFace(Bitmap bmp, Face face) {
        Rect r = face.getBoundingBox();
        return Bitmap.createBitmap(
                bmp,
                Math.max(0, r.left),
                Math.max(0, r.top),
                Math.min(r.width(), bmp.getWidth() - r.left),
                Math.min(r.height(), bmp.getHeight() - r.top)
        );
    }

    private Bitmap normalizeFace(Bitmap face) {
        Bitmap resized = Bitmap.createScaledBitmap(face, 112, 112, true);
        Bitmap gray = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < 112; x++) {
            for (int y = 0; y < 112; y++) {
                int p = resized.getPixel(x, y);
                int g = (int) (0.3 * Color.red(p)
                        + 0.59 * Color.green(p)
                        + 0.11 * Color.blue(p));
                gray.setPixel(x, y, Color.rgb(g, g, g));
            }
        }
        return gray;
    }

    private float calculateSimilarity(Bitmap b1, Bitmap b2) {
        long diff = 0;
        for (int x = 0; x < 112; x++) {
            for (int y = 0; y < 112; y++) {
                diff += Math.abs(
                        Color.red(b1.getPixel(x, y)) -
                                Color.red(b2.getPixel(x, y)));
            }
        }
        return 1f - (diff / (112f * 112f * 255f));
    }

    // ================= FIREBASE =================
    private void saveAttendance() {
        Map<String, Object> map = new HashMap<>();
        map.put("studentId", mAuth.getUid());
        map.put("classId", classId);
        map.put("lat", studentLocation.getLatitude());
        map.put("lng", studentLocation.getLongitude());
        map.put("date", getTodayDate());
        map.put("timestamp", System.currentTimeMillis());

        db.collection("attendance")
                .add(map)
                .addOnSuccessListener(d -> {
                    Toast.makeText(this, "Attendance marked ✅", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadClassDetails() {
        db.collection("classes").document(classId)
                .get()
                .addOnSuccessListener(d -> {
                    classLat = d.getDouble("latitude");
                    classLng = d.getDouble("longitude");
                    classRadius = d.getDouble("radius");
                    tvClassName.setText(d.getString("className"));
                });
    }

    private void loadProfilePhoto() {

        String uid = FirebaseAuth.getInstance().getUid();

        FirebaseFirestore.getInstance()
                .collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "Profile not found",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // 🔥 SAFE CHECK
                    if (!doc.contains("profileImageBase64") ||
                            doc.getString("profileImageBase64") == null ||
                            doc.getString("profileImageBase64").isEmpty()) {

                        Toast.makeText(this,
                                "Upload profile photo first",
                                Toast.LENGTH_LONG).show();

                        finish(); // stop attendance
                        return;
                    }

                    // ✅ SAFE TO DECODE NOW
                    String base64 = doc.getString("profileImageBase64");
                    profileBitmap = base64ToBitmap(base64);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load profile",
                                Toast.LENGTH_SHORT).show()
                );
    }
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
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}
