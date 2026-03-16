package com.example.smart_attendance_system.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.smart_attendance_system.LoginActivity;
import com.example.smart_attendance_system.R;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    public ProfileFragment(){}

    Button Logout;
    FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Logout = view.findViewById(R.id.btnlogoutteacher);
        mAuth = FirebaseAuth.getInstance();

        Logout.setOnClickListener(v -> {

            // SIGN OUT USER
            mAuth.signOut();

            // GO TO LOGIN SCREEN
            Intent intent = new Intent(getContext(), LoginActivity.class);

            // CLEAR BACK STACK (important)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);

        });

        return view;
    }
}