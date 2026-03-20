package com.example.smart_attendance_system.Fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.StartAttendanceActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    LinearLayout classListLayout;
    FirebaseFirestore db;

    // 🔥 STATS
    TextView tvTotalClasses, tvTotalStudents, tvPresent, tvAbsent;

    public DashboardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Buttons
        view.findViewById(R.id.btnCreateClass).setOnClickListener(v -> {
            BottomNavigationView bottomNav =
                    getActivity().findViewById(R.id.bottomNav);
            bottomNav.setSelectedItemId(R.id.nav_classes);
        });

        view.findViewById(R.id.btnStartAttendance).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), StartAttendanceActivity.class)));

        // ✅ INIT
        classListLayout = view.findViewById(R.id.classList);

        tvTotalClasses = view.findViewById(R.id.tvTotalClasses);
        tvTotalStudents = view.findViewById(R.id.tvTotalStudents);
        tvPresent = view.findViewById(R.id.tvPresent);
        tvAbsent = view.findViewById(R.id.tvAbsent);

        db = FirebaseFirestore.getInstance();

        // ✅ LOAD DATA
        loadTodayClasses();
        loadDashboardStats();

        return view;
    }

    // 🔥 LOAD TODAY CLASSES
    private void loadTodayClasses() {

        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        String today = sdf.format(new Date());

        classListLayout.removeAllViews();

        db.collection("classes")
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        showNoClass();
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String className = doc.getString("className");
                        String room = doc.getString("roomName");
                        String time = doc.getString("time");

                        addClassCard(className, room, time);
                    }
                });
    }

    // 🔥 ADD CARD VIEW
    private void addClassCard(String className, String room, String time) {

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 40, 40, 40);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);

        card.setBackgroundResource(R.drawable.bg_card);

        // Fonts
        Typeface boldFont = ResourcesCompat.getFont(getContext(), R.font.poppins_semibold);
        Typeface normalFont = ResourcesCompat.getFont(getContext(), R.font.poppins_medium);

        // Class
        TextView tvClass = new TextView(getContext());
        tvClass.setText("Class : " + (className != null ? className : "N/A"));
        tvClass.setTextSize(18);
        tvClass.setTypeface(boldFont);

        // Room
        TextView tvRoom = new TextView(getContext());
        tvRoom.setText("Room  : " + (room != null ? room : "N/A"));
        tvRoom.setTextSize(14);
        tvRoom.setTypeface(normalFont);

        // Time
        TextView tvTime = new TextView(getContext());
        tvTime.setText("Time  : " + (time != null ? time : "N/A"));
        tvTime.setTextSize(14);
        tvTime.setTypeface(normalFont);

        tvRoom.setPadding(0, 10, 0, 0);
        tvTime.setPadding(0, 5, 0, 0);

        card.addView(tvClass);
        card.addView(tvRoom);
        card.addView(tvTime);

        // Click
        card.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StartAttendanceActivity.class);
            startActivity(intent);
        });

        classListLayout.addView(card);
    }

    // ❌ NO CLASS
    private void showNoClass() {
        TextView tv = new TextView(getContext());
        tv.setText("No classes today");
        tv.setTextSize(16);
        tv.setPadding(20, 20, 20, 20);
        classListLayout.addView(tv);
    }


    private void loadDashboardStats() {

        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        String today = sdf.format(new Date());

        // Total Classes
        db.collection("classes")
                .get()
                .addOnSuccessListener(snapshot ->
                        tvTotalClasses.setText(String.valueOf(snapshot.size()))
                );

        // Total Students
        db.collection("students")
                .get()
                .addOnSuccessListener(snapshot ->
                        tvTotalStudents.setText(String.valueOf(snapshot.size()))
                );

        // Present Today
        db.collection("attendance")
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snapshot -> {

                    int present = snapshot.size();
                    tvPresent.setText(String.valueOf(present));

                    // Absent
                    db.collection("students")
                            .get()
                            .addOnSuccessListener(studentSnap -> {

                                int total = studentSnap.size();
                                int absent = total - present;

                                tvAbsent.setText(String.valueOf(absent));
                            });
                });
    }
}