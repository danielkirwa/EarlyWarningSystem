package com.example.earlywarningsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentManagement extends AppCompatActivity {
    private EditText etName, etPhone, etClass;
    private AutoCompleteTextView autoParentPhone; // NEW: search parent phone
    private Spinner spinnerBorderZone;
    private Button btnAddStudent;
    private ListView lvStudents;

    private ArrayAdapter<String> studentAdapter;
    private List<String> studentList = new ArrayList<>();
    private ArrayAdapter<String> parentAdapter;
    private List<String> parentPhones = new ArrayList<>();

    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_management);

        studentsRef = FirebaseDatabase.getInstance("https://earlywarningsystem-a36d5-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Students");

        initViews();
        setupBorderZoneSpinner();
        loadParents();
        loadAllStudents();

        btnAddStudent.setOnClickListener(v -> addStudent());

        // When user selects/enters a parent phone, show their children
        autoParentPhone.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPhone = (String) parent.getItemAtPosition(position);
            loadStudentsByParent(selectedPhone);
        });
        // text watcher to check back to full list
        autoParentPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s.toString())) {
                    loadAllStudents(); // show all when cleared
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etClass = findViewById(R.id.etClass);
        autoParentPhone = findViewById(R.id.autoParentPhone); // add in XML
        spinnerBorderZone = findViewById(R.id.spinnerBorderZone);
        btnAddStudent = findViewById(R.id.btnAddStudent);
        lvStudents = findViewById(R.id.lvStudents);
    }

    private void setupBorderZoneSpinner() {
        String[] borderZones = {"Interior", "Busia Border", "Malaba Border"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, borderZones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBorderZone.setAdapter(adapter);
    }

    private void loadParents() {
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                parentPhones.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String phone = child.child("parentPhone").getValue(String.class);
                    if (phone != null && !parentPhones.contains(phone)) {
                        parentPhones.add(phone);
                    }
                }
                parentAdapter = new ArrayAdapter<>(StudentManagement.this,
                        android.R.layout.simple_dropdown_item_1line, parentPhones);
                autoParentPhone.setAdapter(parentAdapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(StudentManagement.this, "Failed to load parents", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStudentsByParent(String parentPhone) {
        studentsRef.orderByChild("parentPhone").equalTo(parentPhone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        studentList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Student s = child.getValue(Student.class);
                            if (s != null) {
                                studentList.add(s.name + " - " + s.studentClass);
                            }
                        }
                        studentAdapter = new ArrayAdapter<>(StudentManagement.this,
                                android.R.layout.simple_list_item_1, studentList);
                        lvStudents.setAdapter(studentAdapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(StudentManagement.this, "Failed to load students", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addStudent() {
        String name = etName.getText().toString().trim();
        String phone = autoParentPhone.getText().toString().trim();
        String studentClass = etClass.getText().toString().trim();
        String borderZone = spinnerBorderZone.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter student name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(studentClass)) {
            Toast.makeText(this, "Please enter class", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!phone.isEmpty() && !phone.matches("^\\+254\\d{9}$")) {
            Toast.makeText(this, "Phone must be in format +2547XXXXXXXX", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = studentsRef.push().getKey();
        Student student = new Student(studentId, name, studentClass, phone, borderZone);

        studentsRef.child(studentId).setValue(student).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show();
                etName.setText("");
                autoParentPhone.setText("");
                etClass.setText("");
                spinnerBorderZone.setSelection(0);
                loadParents(); // refresh parent list
                loadAllStudents(); // refresh full list immediately
            } else {
                Toast.makeText(this, "Failed to add student", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Firebase model class
    public static class Student {
        public String id, name, studentClass, parentPhone, borderZone;

        public Student() {} // Needed for Firebase

        public Student(String id, String name, String studentClass,
                       String parentPhone, String borderZone) {
            this.id = id;
            this.name = name;
            this.studentClass = studentClass;
            this.parentPhone = parentPhone;
            this.borderZone = borderZone;
        }
    }

    private void loadAllStudents() {
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Student s = child.getValue(Student.class);
                    if (s != null) {
                        studentList.add(s.name + " - " + s.studentClass + " (" + s.parentPhone + ")");
                    }
                }
                studentAdapter = new ArrayAdapter<>(StudentManagement.this,
                        android.R.layout.simple_list_item_1, studentList);
                lvStudents.setAdapter(studentAdapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(StudentManagement.this, "Failed to load students", Toast.LENGTH_SHORT).show();
            }
        });
    }

}