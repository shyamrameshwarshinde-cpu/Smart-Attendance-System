package com.example.smart_attendance_system;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateClassActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 201;

    private EditText etClassName, etSubject;
    private Spinner spinnerClassroom;
    private Button btnCreateClass, btnPickDate, btnPickTime;
    private TextView tvAddClassLocation, tvGpsStatus;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private List<String> classroomList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private String selectedDate = "";
    private String selectedTime = "";

    // Classroom GPS
    private double classLat;
    private double classLng;
    private float classAccuracy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        etClassName = findViewById(R.id.etClassName);
        etSubject = findViewById(R.id.etSubject);
        spinnerClassroom = findViewById(R.id.spinnerLocations);
        btnCreateClass = findViewById(R.id.btnCreateClass);
        tvAddClassLocation = findViewById(R.id.btnAddNewLocation);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);

        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupSpinner();
        loadClassroomLocations();

        tvAddClassLocation.setOnClickListener(v ->
                startActivity(new Intent(this, AddClassLocationActivity.class))
        );

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());

        btnCreateClass.setOnClickListener(v -> checkPermissionAndFetchLocation());
    }

    // 📅 Date picker
    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    selectedDate = day + "/" + (month + 1) + "/" + year;
                    btnPickDate.setText(selectedDate);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ⏰ Time picker
    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    selectedTime = hour + ":" + String.format("%02d", minute);
                    btnPickTime.setText(selectedTime);
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
        ).show();
    }

    // 🔐 Permission check
    private void checkPermissionAndFetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE
            );
        } else {
            requestFreshLocation();
        }
    }

    // 📍 Fresh GPS (NO cached location)
    private void requestFreshLocation() {

        tvGpsStatus.setText("Getting accurate classroom location...");
        btnCreateClass.setEnabled(false);

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000
        )
                .setMinUpdateDistanceMeters(1)
                .setWaitForAccurateLocation(true)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location == null) return;

                if (location.getAccuracy() > 15) {
                    tvGpsStatus.setText(
                            "Waiting for better GPS...\nAccuracy: "
                                    + (int) location.getAccuracy() + "m"
                    );
                    return;
                }

                classLat = location.getLatitude();
                classLng = location.getLongitude();
                classAccuracy = location.getAccuracy();

                fusedLocationClient.removeLocationUpdates(locationCallback);

                tvGpsStatus.setText(
                        "Location fixed ✅\nAccuracy: "
                                + (int) classAccuracy + "m"
                );

                saveClassToFirestore();
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                getMainLooper()
        );
    }

    // 💾 Save class
    private void saveClassToFirestore() {

        String className = etClassName.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String roomName = spinnerClassroom.getSelectedItem().toString();

        if (className.isEmpty()
                || subject.isEmpty()
                || selectedDate.isEmpty()
                || selectedTime.isEmpty()
                || roomName.equals("Select Classroom")) {

            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            btnCreateClass.setEnabled(true);
            return;
        }

        String teacherId = mAuth.getCurrentUser().getUid();

        Map<String, Object> classMap = new HashMap<>();
        classMap.put("className", className);
        classMap.put("subject", subject);
        classMap.put("roomName", roomName);
        classMap.put("date", selectedDate);
        classMap.put("time", selectedTime);

        // ✅ GPS data
        classMap.put("latitude", classLat);
        classMap.put("longitude", classLng);
        classMap.put("radius", 10); // FINAL realistic radius
        classMap.put("accuracy", classAccuracy);

        classMap.put("teacherId", teacherId);
        classMap.put("createdAt", System.currentTimeMillis());

        db.collection("classes")
                .add(classMap)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Class Created Successfully ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateClass.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 🔹 Spinner
    private void setupSpinner() {
        classroomList.clear();
        classroomList.add("Select Classroom");

        spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                classroomList
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClassroom.setAdapter(spinnerAdapter);
    }

    // 🔹 Load classrooms
    private void loadClassroomLocations() {
        if (mAuth.getCurrentUser() == null) return;

        String teacherId = mAuth.getCurrentUser().getUid();

        db.collection("class_locations")
                .document(teacherId)
                .collection("rooms")
                .get()
                .addOnSuccessListener(snapshot -> {
                    classroomList.clear();
                    classroomList.add("Select Classroom");

                    for (var doc : snapshot.getDocuments()) {
                        classroomList.add(doc.getId());
                    }
                    spinnerAdapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClassroomLocations();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
