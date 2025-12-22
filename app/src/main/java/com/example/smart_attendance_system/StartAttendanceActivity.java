package com.example.smart_attendance_system;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartAttendanceActivity extends AppCompatActivity {

    Spinner spinnerClass;
    Button btnStart;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    List<String> classList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_attendance);

        spinnerClass = findViewById(R.id.spinnerClass);
        btnStart = findViewById(R.id.btnStart);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                classList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(adapter);

        loadTeacherClasses();

        btnStart.setOnClickListener(v -> startAttendance());
    }

    // 🔹 Load teacher classes
    private void loadTeacherClasses() {

        String teacherId = mAuth.getCurrentUser().getUid();

        db.collection("classes")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(query -> {

                    classList.clear();
                    classList.add("Select Class");

                    for (var doc : query.getDocuments()) {
                        classList.add(doc.getString("className"));
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    // 🔹 Start attendance session
    private void startAttendance() {

        String selectedClass = spinnerClass.getSelectedItem().toString();

        if (selectedClass.equals("Select Class")) {
            Toast.makeText(this, "Select a class", Toast.LENGTH_SHORT).show();
            return;
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000); // 10 minutes

        Map<String, Object> session = new HashMap<>();
        session.put("className", selectedClass);
        session.put("teacherId", mAuth.getCurrentUser().getUid());
        session.put("startTime", startTime);
        session.put("endTime", endTime);
        session.put("active", true);

        db.collection("attendance_sessions")
                .add(session)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Attendance Started", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
