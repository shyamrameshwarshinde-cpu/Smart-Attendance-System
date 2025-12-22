package com.example.smart_attendance_system;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class TeacherDashboardActivity extends AppCompatActivity {

    private Button btnCreateClass, btnSmartAttendance, btnManualAttendance, btnExportExcel,btnStartAttendance,btnLogout;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        btnCreateClass = findViewById(R.id.btnCreateClass);
//        btnSmartAttendance = findViewById(R.id.btnSmartAttendance);
        btnManualAttendance = findViewById(R.id.btnManualAttendance);
        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnStartAttendance = findViewById(R.id.btnstartattendance);

        btnLogout = findViewById(R.id.btnLogout);
        mAuth = FirebaseAuth.getInstance();

        btnCreateClass.setOnClickListener(v ->
                startActivity(new Intent(this, CreateClassActivity.class)));

//        btnSmartAttendance.setOnClickListener(v -> {
//            // Navigate to Smart Attendance
//        });
        btnManualAttendance.setOnClickListener(v -> {
            // Navigate to Manual Attendance
        });
        btnExportExcel.setOnClickListener(v -> {
            // Implement Excel export
        });

        btnStartAttendance.setOnClickListener(v -> {
            startActivity(new Intent(
                    TeacherDashboardActivity.this,
                    StartAttendanceActivity.class
            ));
        });


        btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out from Firebase
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Go back to login
            Intent intent = new Intent(TeacherDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close dashboard so back button won't return
        });

    }
}
