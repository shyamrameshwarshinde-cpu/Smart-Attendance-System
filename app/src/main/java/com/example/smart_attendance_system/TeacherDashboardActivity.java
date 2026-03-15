package com.example.smart_attendance_system;

import android.content.Intent;
import android.os.Bundle;

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
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Safety check — if not logged in, go to login
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_teacher_dashboard);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new DashboardFragment())
                    .commit();
        }

        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment());
            } else if (id == R.id.nav_classes) {
                loadFragment(new ClassesFragment());
            } else if (id == R.id.nav_reports) {
                loadFragment(new ReportsFragment());
            } else if (id == R.id.nav_profile) {
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

    // ✅ Call this from ProfileFragment or anywhere to logout
    public void logout() {
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