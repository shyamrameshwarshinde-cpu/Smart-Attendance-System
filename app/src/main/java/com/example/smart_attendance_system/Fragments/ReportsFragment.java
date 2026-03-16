package com.example.smart_attendance_system.Fragments;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.smart_attendance_system.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ReportsFragment extends Fragment {

    Spinner spinnerClass;
    Button btnGenerateReport;

    FirebaseFirestore db;

    List<String> classList = new ArrayList<>();
    List<String> classIdList = new ArrayList<>();

    public ReportsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        spinnerClass = view.findViewById(R.id.spinnerClass);
        btnGenerateReport = view.findViewById(R.id.btnGenerateReport);

        db = FirebaseFirestore.getInstance();

        loadClasses();

        btnGenerateReport.setOnClickListener(v -> generateReport());

        return view;
    }

    private void loadClasses() {

        db.collection("classes")
                .get()
                .addOnSuccessListener(snapshot -> {

                    classList.clear();
                    classIdList.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        classList.add(doc.getString("className"));
                        classIdList.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            classList
                    );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerClass.setAdapter(adapter);
                });
    }

    private void generateReport() {

        int position = spinnerClass.getSelectedItemPosition();
        String selectedClassId = classIdList.get(position);
        String selectedClassName = classList.get(position);

        db.collection("attendance")
                .whereEqualTo("classId", selectedClassId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    try {

                        Workbook workbook = new XSSFWorkbook();
                        Sheet sheet = workbook.createSheet("Attendance");

                        Row header = sheet.createRow(0);

                        header.createCell(0).setCellValue("Student Name");
                        header.createCell(1).setCellValue("Class");
                        header.createCell(2).setCellValue("Date");
                        header.createCell(3).setCellValue("Time");
                        header.createCell(4).setCellValue("Latitude");
                        header.createCell(5).setCellValue("Longitude");

                        final int[] rowIndex = {1};

                        int totalRecords = snapshot.size();
                        final int[] processedRecords = {0};

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {

                            String studentId = doc.getString("studentId");
                            String date = doc.getString("date");

                            Double lat = doc.getDouble("lat");
                            Double lng = doc.getDouble("lng");

                            /* ---- TIMESTAMP FIX (Long → Time) ---- */

                            Long timestampValue = doc.getLong("timestamp");

                            String finalTime = "";

                            if (timestampValue != null) {

                                java.util.Date dateObj = new java.util.Date(timestampValue);

                                java.text.SimpleDateFormat sdf =
                                        new java.text.SimpleDateFormat("HH:mm:ss");

                                finalTime = sdf.format(dateObj);
                            }

                            final String timeValue = finalTime;

                            db.collection("students")
                                    .document(studentId)
                                    .get()
                                    .addOnSuccessListener(studentDoc -> {

                                        try {

                                            String studentName =
                                                    studentDoc.getString("name");

                                            Row row = sheet.createRow(rowIndex[0]++);

                                            row.createCell(0).setCellValue(studentName);
                                            row.createCell(1).setCellValue(selectedClassName);
                                            row.createCell(2).setCellValue(date);
                                            row.createCell(3).setCellValue(timeValue);

                                            if (lat != null)
                                                row.createCell(4).setCellValue(lat);

                                            if (lng != null)
                                                row.createCell(5).setCellValue(lng);

                                            processedRecords[0]++;

                                            if (processedRecords[0] == totalRecords) {

                                                File file = new File(
                                                        Environment.getExternalStoragePublicDirectory(
                                                                Environment.DIRECTORY_DOWNLOADS),
                                                        selectedClassName + "_AttendanceReport.xlsx"
                                                );

                                                FileOutputStream outputStream =
                                                        new FileOutputStream(file);

                                                workbook.write(outputStream);
                                                outputStream.close();

                                                Toast.makeText(getContext(),
                                                        "Report saved in Downloads",
                                                        Toast.LENGTH_LONG).show();
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    });

                        }

                    } catch (Exception e) {

                        Toast.makeText(getContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                });
    }}