package com.example.smart_attendance_system;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Spinner spinnerRole;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    AutoCompleteTextView dropRole;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        dropRole = findViewById(R.id.dropRole);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        AutoCompleteTextView dropRole = findViewById(R.id.dropRole);
        btnRegister = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();



        String[] roles = {"Student", "Teacher"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1,
                        roles);

        dropRole.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = dropRole.getText().toString().toLowerCase();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String uid = mAuth.getCurrentUser().getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("role", role);

                        db.collection("users")
                                .document(uid)
                                .set(user)
                                .addOnSuccessListener(unused -> {

                                    Toast.makeText(this,
                                            "Registration Successful",
                                            Toast.LENGTH_SHORT).show();

                                    startActivity(new Intent(
                                            RegistrationActivity.this,
                                            LoginActivity.class));

                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "Firestore Error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();

                                    btnRegister.setEnabled(true);
                                });

                    } else {

                        Toast.makeText(this,
                                "Registration Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();

                        btnRegister.setEnabled(true);
                    }
                });
    }}
