package com.example.smart_attendance_system;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddClassLocationActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 101;

    EditText etClassroomName;
    Button btnGetLocation, btnSaveLocation;
    TextView tvGpsStatus;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FusedLocationProviderClient fusedLocationClient;

    Location currentLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class_location);

        // UI
        etClassroomName = findViewById(R.id.etLocationName);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnSaveLocation = findViewById(R.id.btnSaveLocation);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);

        // Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get location
        btnGetLocation.setOnClickListener(v -> {
            tvGpsStatus.setText("Fetching location...");
            checkPermissionAndFetchLocation();
        });

        // Save location
        btnSaveLocation.setOnClickListener(v -> saveLocation());
    }

    // 🔹 Check permission
    private void checkPermissionAndFetchLocation() {

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE
            );

        } else {
            fetchLocation();
        }
    }

    // 🔹 Fetch location
    private void fetchLocation() {

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location != null) {

                        currentLocation = location;

                        tvGpsStatus.setText(
                                "Latitude: " + location.getLatitude() +
                                        "\nLongitude: " + location.getLongitude() +
                                        "\nAccuracy: " + location.getAccuracy() + " meters"
                        );

                    } else {
                        tvGpsStatus.setText("Unable to get location. Turn ON GPS.");
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Location error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    // 🔹 Save location to Firestore
    private void saveLocation() {

        String roomName = etClassroomName.getText().toString().trim();

        if (roomName.isEmpty()) {
            Toast.makeText(this, "Enter classroom name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLocation == null) {
            Toast.makeText(this, "Get location first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Teacher not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = mAuth.getCurrentUser().getUid();

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("roomName", roomName);
        locationMap.put("latitude", currentLocation.getLatitude());
        locationMap.put("longitude", currentLocation.getLongitude());

        db.collection("class_locations")
                .document(teacherId)
                .collection("rooms")
                .document(roomName)
                .set(locationMap)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this,
                            "Classroom location saved",
                            Toast.LENGTH_SHORT).show();

                    finish(); // go back

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Firestore error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    // 🔹 Permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            fetchLocation();

        } else {

            Toast.makeText(this,
                    "Location permission required",
                    Toast.LENGTH_SHORT).show();
        }
    }
}