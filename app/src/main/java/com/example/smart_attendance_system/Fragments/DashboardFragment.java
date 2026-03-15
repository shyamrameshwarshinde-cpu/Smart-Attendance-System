package com.example.smart_attendance_system.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.smart_attendance_system.CreateClassActivity;
import com.example.smart_attendance_system.LoginActivity;
import com.example.smart_attendance_system.R;
import com.example.smart_attendance_system.StartAttendanceActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardFragment extends Fragment {

    Button btnCreateClass, btnStartAttendance, btnReports, btnProfile, btnLogout;
    FirebaseAuth mAuth;

    public DashboardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // ✅ CONNECT IDS (VERY IMPORTANT)
        btnCreateClass = view.findViewById(R.id.btnCreateClass);
        btnStartAttendance = view.findViewById(R.id.btnStartAttendance);
        btnReports = view.findViewById(R.id.btnReports);
        btnProfile = view.findViewById(R.id.btnProfile);
//        btnLogout = view.findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();

        // ✅ CLICK EVENTS

        btnCreateClass.setOnClickListener(v -> {

            BottomNavigationView bottomNav =
                    getActivity().findViewById(R.id.bottomNav);

            bottomNav.setSelectedItemId(R.id.nav_classes);

        });

        btnStartAttendance.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), StartAttendanceActivity.class)));
//
//        btnLogout.setOnClickListener(v -> {
//            mAuth.signOut();
//            startActivity(new Intent(getActivity(), LoginActivity.class));
//            requireActivity().finish();
//        });

        return view;
    }
}