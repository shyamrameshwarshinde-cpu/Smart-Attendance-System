package com.example.smart_attendance_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin, btnGoRegister;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Auto-login: if already logged in, skip login screen
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            redirectUser(mAuth.getCurrentUser().getUid());
            return; // don't load login UI
        }

        setContentView(R.layout.activity_login);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> loginUser());

        btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrationActivity.class))
        );
    }

    private void loginUser() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    redirectUser(uid);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this,
                            "Login failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void redirectUser(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this,
                                "User data not found. Please register again.",
                                Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        btnLogin.setEnabled(true);
                        return;
                    }

                    String role = doc.getString("role");

                    Intent intent;
                    if ("teacher".equals(role)) {
                        intent = new Intent(this, TeacherDashboardActivity.class);
                    } else {
                        intent = new Intent(this, StudentDashboardActivity.class);
                    }

                    // ✅ Clear back stack so user can't go back to login
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to fetch role: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    if (btnLogin != null) btnLogin.setEnabled(true);
                });
    }
}