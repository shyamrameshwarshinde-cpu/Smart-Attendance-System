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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

                        // ✅ HEADER
                        Row header = sheet.createRow(0);
                        header.createCell(0).setCellValue("Student Name");
                        header.createCell(1).setCellValue("Enrollment No");
                        header.createCell(2).setCellValue("Class");
                        header.createCell(3).setCellValue("Date");
                        header.createCell(4).setCellValue("Timestamp");

                        final int total = snapshot.size();
                        final int[] rowIndex = {1};
                        final int[] completed = {0};

                        if (total == 0) {
                            Toast.makeText(getContext(), "No attendance found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {

                            String studentId = doc.getString("studentId");
                            String date = doc.getString("date");

                            // ✅ FINAL DATE
                            final String finalDate = date != null ? date : "";

                            // ✅ FINAL TIME
                            Long timestampLong = doc.getLong("timestamp");
                            final String finalTime;

                            if (timestampLong != null) {
                                Date dateObj = new Date(timestampLong);
                                SimpleDateFormat sdf =
                                        new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                finalTime = sdf.format(dateObj);
                            } else {
                                finalTime = "";
                            }

                            // 🔥 Fetch student details
                            db.collection("students")
                                    .document(studentId)
                                    .get()
                                    .addOnSuccessListener(studentDoc -> {

                                        try {

                                            String studentName = studentDoc.getString("name");
                                            String enrollmentNo = studentDoc.getString("enrollment");

                                            Row row = sheet.createRow(rowIndex[0]++);

                                            row.createCell(0).setCellValue(
                                                    studentName != null ? studentName : "");

                                            row.createCell(1).setCellValue(
                                                    enrollmentNo != null ? enrollmentNo : "");

                                            row.createCell(2).setCellValue(selectedClassName);
                                            row.createCell(3).setCellValue(finalDate);
                                            row.createCell(4).setCellValue(finalTime);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        // ✅ TRACK COMPLETION (IMPORTANT FIX)
                                        completed[0]++;

                                        if (completed[0] == total) {
                                            saveExcel(workbook, selectedClassName);
                                        }
                                    })
                                    .addOnFailureListener(e -> {

                                        completed[0]++;

                                        if (completed[0] == total) {
                                            saveExcel(workbook, selectedClassName);
                                        }
                                    });
                        }

                    } catch (Exception e) {

                        Toast.makeText(getContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveExcel(Workbook workbook, String className) {

        try {

            File file = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS),
                    className + "_AttendanceReport.xlsx"
            );

            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            outputStream.close();

            Toast.makeText(getContext(),
                    "Report saved in Downloads",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {

            Toast.makeText(getContext(),
                    "Save Error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}