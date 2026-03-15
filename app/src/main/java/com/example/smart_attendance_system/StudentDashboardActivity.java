package com.example.smart_attendance_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnViewClasses, btnProfile, btnLogout;
    private ListView lvClasses;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ArrayList<ClassItem> classList = new ArrayList<>();
    private ArrayAdapter<ClassItem> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        tvWelcome     = findViewById(R.id.tvWelcome);
        btnViewClasses = findViewById(R.id.btnViewClasses);
        btnProfile    = findViewById(R.id.btnProfile);
        btnLogout     = findViewById(R.id.btnLogout);
        lvClasses     = findViewById(R.id.lvClasses);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // ✅ Safety check — if not logged in, go to login
        if (mAuth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                classList
        );
        lvClasses.setAdapter(adapter);

        // ✅ Show name instead of email
        loadUserName();

        btnViewClasses.setOnClickListener(v -> loadTodaysClasses());

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("mode", "view");
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());

        lvClasses.setOnItemClickListener((parent, view, position, id) -> {
            ClassItem selectedClass = classList.get(position);

            if (selectedClass == null || selectedClass.classId == null) {
                Toast.makeText(this, "Invalid class", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, MarkAttendanceActivity.class);
            intent.putExtra("classId", selectedClass.classId);
            startActivity(intent);
        });

        loadTodaysClasses();
    }

    // ✅ Load name from Firestore
    private void loadUserName() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String name = doc.getString("name");
                        tvWelcome.setText("Welcome, " +
                                (name != null ? name : mAuth.getCurrentUser().getEmail()));
                    }
                });
    }

    private void loadTodaysClasses() {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        String today = sdf.format(Calendar.getInstance().getTime());

        classList.clear();
        adapter.notifyDataSetChanged();

        db.collection("classes")
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        String classId   = doc.getId();
                        String className = doc.getString("className");
                        String subject   = doc.getString("subject");
                        classList.add(new ClassItem(classId,
                                className + " (" + subject + ")"));
                    }

                    adapter.notifyDataSetChanged();

                    if (classList.isEmpty()) {
                        Toast.makeText(this,
                                "No classes today",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load classes",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void logout() {
        mAuth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}