package com.example.earlywarningsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Reports extends AppCompatActivity {
    private ListView lvReports;
    private List<Student> students = new ArrayList<>();

    private DatabaseReference studentsRef;
    private DatabaseReference attendanceRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        lvReports = findViewById(R.id.lvReports);

        studentsRef = FirebaseDatabase.getInstance(
                "https://earlywarningsystem-a36d5-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("Students");

        attendanceRef = FirebaseDatabase.getInstance(
                "https://earlywarningsystem-a36d5-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("Attendance");

        loadReports();
    }

    private void loadReports() {
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                students.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Student s = child.getValue(Student.class);
                    if (s != null) students.add(s);
                }
                lvReports.setAdapter(new ReportAdapter());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Reports.this, "Failed to load students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAttendanceStats(String studentId, TextView tvStats, TextView tvRiskStatus, View rowView) {
        attendanceRef.orderByChild("studentId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int total = 0, present = 0, absent = 0;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            AttendanceRecord record = child.getValue(AttendanceRecord.class);
                            if (record != null && record.status != null) {
                                String status = record.status.trim().toLowerCase();
                                if (status.contains("present") || status.contains("✓")) present++;
                                else if (status.contains("absent") || status.contains("✗")) absent++;
                                total++;
                            }
                        }

                        double rate = total > 0 ? (present * 100.0 / total) : 0.0;
                        String statsText = String.format("Last 30 days: %d/%d days present (%.1f%%)\nAbsent: %d days",
                                present, total, rate, absent);
                        tvStats.setText(statsText);

                        // Risk evaluation
                        //boolean risk = rate < 70.0 || absent >= 5;
                        boolean risk =  absent >= 5;
                        if (risk) {
                            tvRiskStatus.setText("⚠ AT RISK");
                            tvRiskStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            rowView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                        } else {
                            tvRiskStatus.setText("✓ Normal");
                            tvRiskStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            rowView.setBackgroundColor(getResources().getColor(android.R.color.white));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvStats.setText("Failed to load stats");
                    }
                });
    }

    private class ReportAdapter extends ArrayAdapter<Student> {
        public ReportAdapter() {
            super(Reports.this, R.layout.report_item, students);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.report_item, parent, false);
            }

            Student student = students.get(position);

            TextView tvName = convertView.findViewById(R.id.tvReportStudentName);
            TextView tvClass = convertView.findViewById(R.id.tvReportClass);
            TextView tvStats = convertView.findViewById(R.id.tvReportStats);
            TextView tvRiskStatus = convertView.findViewById(R.id.tvRiskStatus);

            tvName.setText(student.name);
            tvClass.setText(student.studentClass);

            // Load stats + risk dynamically
            loadAttendanceStats(student.id, tvStats, tvRiskStatus, convertView);

            return convertView;
        }
    }




    private void loadAttendanceStats(String studentId, TextView tvStats) {
        attendanceRef.orderByChild("studentId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int total = 0, present = 0, absent = 0;
                        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

                        for (DataSnapshot child : snapshot.getChildren()) {
                            AttendanceRecord record = child.getValue(AttendanceRecord.class);
                            if (record != null && record.status != null) {
                                String status = record.status.trim().toLowerCase();

                                // Normalize symbols and text
                                if (status.contains("present") || status.contains("✓")) {
                                    present++;
                                } else if (status.contains("absent") || status.contains("✗")) {
                                    absent++;
                                }
                                total++;
                            }
                        }

                        double rate = total > 0 ? (present * 100.0 / total) : 0.0;
                        String statsText = String.format("Last 30 days: %d/%d days present (%.1f%%)\nAbsent: %d days",
                                present, total, rate, absent);
                        tvStats.setText(statsText);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvStats.setText("Failed to load stats");
                    }
                });
    }

}