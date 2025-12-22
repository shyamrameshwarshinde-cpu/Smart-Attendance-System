package com.example.smart_attendance_system;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

    private EditText etClassroomName;
    private Button btnSaveLocation,BtnNext;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean locationSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class_location);



        etClassroomName = findViewById(R.id.etClassroomName);
        btnSaveLocation = findViewById(R.id.btnSaveLocation);
        BtnNext = findViewById(R.id.btnNext);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnSaveLocation.setOnClickListener(v -> checkPermissionAndFetchLocation());

        BtnNext.setOnClickListener(v -> {
            if (!locationSaved) {
                Toast.makeText(this, "Save location first", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ GO BACK TO CREATE CLASS
            finish();
        });



    }

    // 🔹 Permission check
    private void checkPermissionAndFetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE
            );
        } else {
            fetchLocation();
        }
    }

    // 🔹 Get GPS location
    private void fetchLocation() {

        String roomName = etClassroomName.getText().toString().trim();

        if (roomName.isEmpty()) {
            Toast.makeText(this, "Enter classroom name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Teacher not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveLocationToFirestore(location, roomName);
                    } else {
                        Toast.makeText(this, "Turn ON GPS and try again", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // 🔹 Save to Firestore
    private void saveLocationToFirestore(Location location, String roomName) {

        String teacherId = mAuth.getCurrentUser().getUid();

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("roomName", roomName);
        locationMap.put("latitude", location.getLatitude());
        locationMap.put("longitude", location.getLongitude());
        locationMap.put("radius", 20); // meters (classroom size)

        db.collection("class_locations")
                .document(teacherId)
                .collection("rooms")
                .document(roomName)
                .set(locationMap)
                .addOnSuccessListener(unused -> {
                    locationSaved = true;
                    Toast.makeText(this, "Classroom location saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
}
