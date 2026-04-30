package com.example.earlywarningsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class Dashboard extends AppCompatActivity {
    private TextView tvTotalStudents, tvAtRiskStudents, tvActiveAlerts;
    private Button btnMarkAttendance, btnManageStudents, btnViewAlerts, btnReports, btnLogout;
    FirebaseAuth mAuth;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        tvTotalStudents = findViewById(R.id.tvTotalStudents);
        tvAtRiskStudents = findViewById(R.id.tvAtRiskStudents);
        tvActiveAlerts = findViewById(R.id.tvActiveAlerts);

        btnMarkAttendance = findViewById(R.id.btnMarkAttendance);
        btnManageStudents = findViewById(R.id.btnManageStudents);
        btnViewAlerts = findViewById(R.id.btnViewAlerts);
        btnReports = findViewById(R.id.btnReports);
        btnLogout = findViewById(R.id.btnLogout);
        // call of other methods

        setClickListeners();

        btnLogout.setOnClickListener(v -> {
            // Sign out from Firebase
            mAuth.signOut();

            // Clear Remember Me flag
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("remember", false);
            editor.apply();

            Toast.makeText(Dashboard.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

            // Redirect to LoginActivity
            Intent intent = new Intent(Dashboard.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

// Outside main class
    private void setClickListeners() {
        btnMarkAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Dashboard.this, Attendance.class));
            }
        });

        btnManageStudents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Dashboard.this, StudentManagement.class));
            }
        });

        btnViewAlerts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Dashboard.this, Alerts.class));
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Dashboard.this, Reports.class));
            }
        });
    }
}