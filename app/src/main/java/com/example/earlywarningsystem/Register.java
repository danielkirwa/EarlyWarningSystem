package com.example.earlywarningsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Register extends AppCompatActivity {
    TextInputEditText etFirstName, etLastName, etEmail, etPhone, etPassword, etConfirmPassword;
    Button btnRegister;
    TextView tvBackLogin;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Set status bar color to blue
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        getWindow().getDecorView().setSystemUiVisibility(0);

        // Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackLogin = findViewById(R.id.tvBackLogin);

        // Register button logic
        btnRegister.setOnClickListener(v -> {
            String fname = etFirstName.getText().toString().trim();
            String lname = etLastName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            if (!pass.equals(confirm)) {
                etConfirmPassword.setError("Passwords do not match");
            } else {
                mAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Format email for Firebase key
                                String formattedEmail = email.replace(".", "_");

                                // Prepare user details
                                HashMap<String, Object> userMap = new HashMap<>();
                                userMap.put("firstName", fname);
                                userMap.put("lastName", lname);
                                userMap.put("email", email);
                                userMap.put("phone", phone);
                                userMap.put("status", "inactive");

                                // Save to Realtime Database using formatted email
                                DatabaseReference ref = FirebaseDatabase.getInstance("https://earlywarningsystem-a36d5-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                        .getReference("Users")
                                        .child(formattedEmail);

                                ref.setValue(userMap).addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(Register.this,
                                                "Registration successful!", Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(Register.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(Register.this,
                                                "Failed to save user details", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(Register.this,
                                        "Registration failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        // Back to Login link
        tvBackLogin.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
