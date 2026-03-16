package com.example.smart_attendance_system.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smart_attendance_system.LoginActivity;
import com.example.smart_attendance_system.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    public ProfileFragment() {}

    // ── UI ──
    private ImageView ivTeacherAvatar;
    private TextView tvTeacherName;
    private TextView tvClassCount, tvStudentCount, tvAttendanceCount;
    private EditText etTeacherName, etTeacherEmail;
    private EditText etDepartment, etSubject, etEmployeeId;
    private Button btnEditTeacherProfile, btnSaveTeacherProfile, btnlogoutteacher;

    // ── Firebase ──
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // ── Bind Views ──
        ivTeacherAvatar       = view.findViewById(R.id.ivTeacherAvatar);
        tvTeacherName         = view.findViewById(R.id.tvTeacherName);
        tvClassCount          = view.findViewById(R.id.tvClassCount);
        tvStudentCount        = view.findViewById(R.id.tvStudentCount);
        tvAttendanceCount     = view.findViewById(R.id.tvAttendanceCount);
        etTeacherName         = view.findViewById(R.id.etTeacherName);
        etTeacherEmail        = view.findViewById(R.id.etTeacherEmail);
        etDepartment          = view.findViewById(R.id.etDepartment);
        etSubject             = view.findViewById(R.id.etSubject);
        etEmployeeId          = view.findViewById(R.id.etEmployeeId);
        btnEditTeacherProfile = view.findViewById(R.id.btnEditTeacherProfile);
        btnSaveTeacherProfile = view.findViewById(R.id.btnSaveTeacherProfile);
        btnlogoutteacher      = view.findViewById(R.id.btnlogoutteacher);

        // ── Firebase ──
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // ── Safety check ──
        if (mAuth.getCurrentUser() == null) {
            goToLogin();
            return view;
        }

        uid = mAuth.getCurrentUser().getUid();

        // ── Set email (always read-only) ──
        etTeacherEmail.setText(mAuth.getCurrentUser().getEmail());

        // ── Start in view mode ──
        setViewMode();

        // ── Load profile + stats ──
        loadProfile();
        loadStats();

        // ── Button Listeners ──
        btnEditTeacherProfile.setOnClickListener(v -> enableEditMode());

        btnSaveTeacherProfile.setOnClickListener(v -> saveProfile());

        btnlogoutteacher.setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });

        return view;
    }

    // ════════════════════════════════
    //         VIEW / EDIT MODE
    // ════════════════════════════════

    private void setViewMode() {
        etTeacherName.setEnabled(false);
        etDepartment.setEnabled(false);
        etSubject.setEnabled(false);
        etEmployeeId.setEnabled(false);
        btnSaveTeacherProfile.setEnabled(false);
        btnEditTeacherProfile.setEnabled(true);
    }

    private void enableEditMode() {
        etTeacherName.setEnabled(true);
        etDepartment.setEnabled(true);
        etSubject.setEnabled(true);
        etEmployeeId.setEnabled(true);
        btnSaveTeacherProfile.setEnabled(true);
        Toast.makeText(getContext(),
                "Edit mode enabled",
                Toast.LENGTH_SHORT).show();
    }

    // ════════════════════════════════
    //         LOAD PROFILE
    // ════════════════════════════════

    private void loadProfile() {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;

                    String name       = doc.getString("name");
                    String department = doc.getString("department");
                    String subject    = doc.getString("subject");
                    String employeeId = doc.getString("employeeId");

                    // Set header name
                    if (name != null) {
                        tvTeacherName.setText(name);
                        etTeacherName.setText(name);
                    }

                    if (department != null) etDepartment.setText(department);
                    if (subject    != null) etSubject.setText(subject);
                    if (employeeId != null) etEmployeeId.setText(employeeId);

                    setViewMode();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to load profile",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ════════════════════════════════
    //         LOAD STATS
    // ════════════════════════════════

    private void loadStats() {
        // Count classes created by this teacher
        db.collection("classes")
                .whereEqualTo("teacherId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    int classCount = snap.size();
                    tvClassCount.setText(String.valueOf(classCount));

                    // Count total attendance records across teacher's classes
                    if (classCount == 0) {
                        tvAttendanceCount.setText("0");
                        return;
                    }

                    // Collect all classIds
                    java.util.List<String> classIds = new java.util.ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        classIds.add(doc.getId());
                    }

                    // Count attendance records for these classes
                    db.collection("attendance")
                            .whereIn("classId", classIds.subList(0,
                                    Math.min(classIds.size(), 10)))
                            .get()
                            .addOnSuccessListener(attSnap ->
                                    tvAttendanceCount.setText(
                                            String.valueOf(attSnap.size()))
                            );
                });

        // Count unique students in attendance
        db.collection("attendance")
                .get()
                .addOnSuccessListener(snap -> {
                    java.util.Set<String> uniqueStudents = new java.util.HashSet<>();
                    for (var doc : snap.getDocuments()) {
                        String sid = doc.getString("studentId");
                        if (sid != null) uniqueStudents.add(sid);
                    }
                    tvStudentCount.setText(String.valueOf(uniqueStudents.size()));
                });
    }

    // ════════════════════════════════
    //         SAVE PROFILE
    // ════════════════════════════════

    private void saveProfile() {
        String name       = etTeacherName.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String subject    = etSubject.getText().toString().trim();
        String employeeId = etEmployeeId.getText().toString().trim();

        // ── Validation ──
        if (name.isEmpty()) {
            etTeacherName.setError("Name is required");
            etTeacherName.requestFocus();
            return;
        }

        if (department.isEmpty()) {
            etDepartment.setError("Department is required");
            etDepartment.requestFocus();
            return;
        }

        if (subject.isEmpty()) {
            etSubject.setError("Subject is required");
            etSubject.requestFocus();
            return;
        }

        if (employeeId.isEmpty()) {
            etEmployeeId.setError("Employee ID is required");
            etEmployeeId.requestFocus();
            return;
        }

        btnSaveTeacherProfile.setEnabled(false);

        Map<String, Object> map = new HashMap<>();
        map.put("name",       name);
        map.put("department", department);
        map.put("subject",    subject);
        map.put("employeeId", employeeId);
        map.put("role",       "teacher");

        db.collection("users")
                .document(uid)
                .set(map, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // Update header name immediately
                    tvTeacherName.setText(name);

                    Toast.makeText(getContext(),
                            "✅ Profile saved successfully",
                            Toast.LENGTH_SHORT).show();

                    setViewMode();
                })
                .addOnFailureListener(e -> {
                    btnSaveTeacherProfile.setEnabled(true);
                    Toast.makeText(getContext(),
                            "Save failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ════════════════════════════════
    //         LOGOUT
    // ════════════════════════════════

    private void goToLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}