package com.example.smart_attendance_system.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.smart_attendance_system.AddClassLocationActivity;
import com.example.smart_attendance_system.LoginActivity;
import com.example.smart_attendance_system.R;
public class ProfileFragment extends Fragment {

    public ProfileFragment(){}

    Button Logout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Logout = view.findViewById(R.id.btnlogoutteacher);

        Logout.setOnClickListener(v ->
                startActivity(new Intent(getContext(), LoginActivity.class))
        );
        return view;


    }
}