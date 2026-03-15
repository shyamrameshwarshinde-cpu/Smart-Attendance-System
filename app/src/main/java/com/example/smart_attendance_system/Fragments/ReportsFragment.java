package com.example.smart_attendance_system.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.smart_attendance_system.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.poi.sl.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;

public class ReportsFragment extends Fragment {

    public ReportsFragment(){}
    Spinner spinnerClass;
    Button btnGenerateExcel;
    FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        spinnerClass = view.findViewById(R.id.spinnerClass);
        btnGenerateExcel = view.findViewById(R.id.btnGenerateReport);

        db = FirebaseFirestore.getInstance();

//        btnGenerateExcel.setOnClickListener(v -> generateExcel());

        return view;
    }

//    private void generateExcel() {
//
//        String selectedClass = spinnerClass.getSelectedItem().toString();
//
//        db.collection("attendance")
//                .whereEqualTo("className", selectedClass)
//                .get()
//                .addOnSuccessListener(snapshot -> {
//
//                    try {
//
//                        Workbook workbook = new XSSFWorkbook();
//                        Sheet sheet = workbook.createSheet("Attendance");
//
//                        Row header = sheet.createRow(0);
//
//                        header.createCell(0).setCellValue("Roll No");
//                        header.createCell(1).setCellValue("Student Name");
//                        header.createCell(2).setCellValue("Class");
//                        header.createCell(3).setCellValue("Date");
//                        header.createCell(4).setCellValue("Time");
//                        header.createCell(5).setCellValue("Status");
//
//                        int rowIndex = 1;
//
//                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
//
//                            Row row = sheet.createRow(rowIndex++);
//
//                            row.createCell(0).setCellValue(doc.getString("rollNo"));
//                            row.createCell(1).setCellValue(doc.getString("studentName"));
//                            row.createCell(2).setCellValue(doc.getString("className"));
//                            row.createCell(3).setCellValue(doc.getString("date"));
//                            row.createCell(4).setCellValue(doc.getString("time"));
//                            row.createCell(5).setCellValue(doc.getString("status"));
//                        }
//
//                        saveExcel(workbook);
//
//                    } catch (Exception e) {
//                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
//                    }
//
//                });
//    }
    private void saveExcel(Workbook workbook) {

        try {

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File file = new File(path, "Attendance_Report.xlsx");

            FileOutputStream outputStream = new FileOutputStream(file);

            workbook.write(outputStream);

            outputStream.close();

            Toast.makeText(getContext(), "Excel Saved in Downloads", Toast.LENGTH_LONG).show();

        } catch (Exception e) {

            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}