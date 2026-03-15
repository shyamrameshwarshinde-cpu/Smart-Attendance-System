package com.example.smart_attendance_system;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.smart_attendance_system.Fragments.ClassesFragment;
import com.example.smart_attendance_system.Fragments.DashboardFragment;
import com.example.smart_attendance_system.Fragments.ProfileFragment;
import com.example.smart_attendance_system.Fragments.ReportsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class TeacherDashboardActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;
    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new DashboardFragment())
                    .commit();
        }


        bottomNav = findViewById(R.id.bottomNav);


        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment());
            } else if (item.getItemId() == R.id.nav_classes) {
                loadFragment(new ClassesFragment());
            } else if (item.getItemId() == R.id.nav_reports) {
                loadFragment(new ReportsFragment());
            } else if (item.getItemId() == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
            }

            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }


    }

