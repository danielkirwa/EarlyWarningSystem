package com.example.earlywarningsystem;

public class Student {
    public String id;
    public String name;
    public String studentClass;
    public String parentPhone;
    public String borderZone;
    public boolean isAtRisk; // dropout risk flag

    // Empty constructor required for Firebase deserialization
    public Student() {}

    // Optional convenience constructor
    public Student(String id, String name, String studentClass,
                   String parentPhone, String borderZone, boolean isAtRisk) {
        this.id = id;
        this.name = name;
        this.studentClass = studentClass;
        this.parentPhone = parentPhone;
        this.borderZone = borderZone;
        this.isAtRisk = isAtRisk;
    }
}
