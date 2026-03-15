package com.example.smart_attendance_system.Fragments;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.smart_attendance_system.AddClassLocationActivity;
import com.example.smart_attendance_system.R;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class ClassesFragment extends Fragment {

    private static final int LOCATION_REQUEST_CODE = 201;

    private EditText etClassName, etSubject;
    private Spinner spinnerClassroom;
    private Button btnCreateClass, btnPickDate, btnPickTime;
    private TextView tvAddClassLocation, tvGpsStatus;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private List<String> classroomList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private String selectedDate = "";
    private String selectedTime = "";

    private double classLat;
    private double classLng;
    private float classAccuracy;

    public ClassesFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_classes, container, false);

        etClassName = view.findViewById(R.id.etClassName);
        etSubject = view.findViewById(R.id.etSubject);
        spinnerClassroom = view.findViewById(R.id.spinnerLocations);

        btnCreateClass = view.findViewById(R.id.btnCreateClass);
        btnPickDate = view.findViewById(R.id.btnPickDate);
        btnPickTime = view.findViewById(R.id.btnPickTime);

        tvAddClassLocation = view.findViewById(R.id.btnAddNewLocation);
        tvGpsStatus = view.findViewById(R.id.tvGpsStatus);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireActivity());

        setupSpinner();
        loadClassroomLocations();

        tvAddClassLocation.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddClassLocationActivity.class))
        );

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnCreateClass.setOnClickListener(v -> checkPermissionAndFetchLocation());

        return view;
    }

    // DATE PICKER
    private void showDatePicker() {

        Calendar c = Calendar.getInstance();

        new DatePickerDialog(
                getContext(),
                (view, year, month, day) -> {

                    selectedDate = day + "/" + (month + 1) + "/" + year;
                    btnPickDate.setText(selectedDate);

                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // TIME PICKER
    private void showTimePicker(){

        Calendar c = Calendar.getInstance();

        new TimePickerDialog(
                getContext(),
                (view, hour, minute) -> {

                    selectedTime = hour + ":" + String.format("%02d", minute);
                    btnPickTime.setText(selectedTime);

                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
        ).show();
    }

    // PERMISSION
    private void checkPermissionAndFetchLocation(){

        if(ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);

        }else{
            requestFreshLocation();
        }
    }

    // LOCATION
    private void requestFreshLocation(){

        tvGpsStatus.setText("Getting accurate classroom location...");
        btnCreateClass.setEnabled(false);

        LocationRequest locationRequest =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        2000)
                        .setMinUpdateDistanceMeters(1)
                        .setWaitForAccurateLocation(true)
                        .build();

        locationCallback = new LocationCallback(){

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult){

                Location location = locationResult.getLastLocation();
                if(location == null) return;

                if(location.getAccuracy() > 15){

                    tvGpsStatus.setText(
                            "Waiting for better GPS...\nAccuracy: "
                                    + (int)location.getAccuracy() + "m");

                    return;
                }

                classLat = location.getLatitude();
                classLng = location.getLongitude();
                classAccuracy = location.getAccuracy();

                fusedLocationClient.removeLocationUpdates(locationCallback);

                tvGpsStatus.setText(
                        "Location fixed ✅\nAccuracy: "
                                + (int)classAccuracy + "m");

                saveClassToFirestore();
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                requireActivity().getMainLooper());
    }

    // SAVE CLASS
    private void saveClassToFirestore(){

        String className = etClassName.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String roomName = spinnerClassroom.getSelectedItem().toString();

        if(className.isEmpty() ||
                subject.isEmpty() ||
                selectedDate.isEmpty() ||
                selectedTime.isEmpty() ||
                roomName.equals("Select Classroom")){

            Toast.makeText(getContext(),"Please fill all fields",Toast.LENGTH_SHORT).show();
            btnCreateClass.setEnabled(true);
            return;
        }

        String teacherId = mAuth.getCurrentUser().getUid();

        Map<String,Object> classMap = new HashMap<>();

        classMap.put("className",className);
        classMap.put("subject",subject);
        classMap.put("roomName",roomName);
        classMap.put("date",selectedDate);
        classMap.put("time",selectedTime);

        classMap.put("latitude",classLat);
        classMap.put("longitude",classLng);
        classMap.put("radius",10);
        classMap.put("accuracy",classAccuracy);

        classMap.put("teacherId",teacherId);
        classMap.put("createdAt",System.currentTimeMillis());

        db.collection("classes")
                .add(classMap)
                .addOnSuccessListener(doc->{

                    Toast.makeText(getContext(),
                            "Class Created Successfully ✅",
                            Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e->{

                    btnCreateClass.setEnabled(true);
                    Toast.makeText(getContext(),
                            e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // SPINNER
    private void setupSpinner(){

        classroomList.clear();
        classroomList.add("Select Classroom");

        spinnerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                classroomList);

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinnerClassroom.setAdapter(spinnerAdapter);
    }

    // LOAD CLASSROOMS
    private void loadClassroomLocations(){

        if(mAuth.getCurrentUser()==null) return;

        String teacherId = mAuth.getCurrentUser().getUid();

        db.collection("class_locations")
                .document(teacherId)
                .collection("rooms")
                .get()
                .addOnSuccessListener(snapshot->{

                    classroomList.clear();
                    classroomList.add("Select Classroom");

                    for(var doc : snapshot.getDocuments()){
                        classroomList.add(doc.getId());
                    }

                    spinnerAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onResume(){
        super.onResume();
        loadClassroomLocations();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if(locationCallback!=null){
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}