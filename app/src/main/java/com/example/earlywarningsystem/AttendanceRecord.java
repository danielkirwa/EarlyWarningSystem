package com.example.earlywarningsystem;

public class AttendanceRecord {
    public String id;
    public String studentId;
    public String date;       // e.g. "2026-05-06"
    public String status;     // "Present", "Absent", "Late"
    public double latitude;
    public double longitude;
    public boolean crossedBorder;
    public String locationName; // human-readable address

    // Empty constructor required for Firebase
    public AttendanceRecord() {}

    // Optional convenience constructor
    public AttendanceRecord(String id, String studentId, String date, String status,
                            double latitude, double longitude,
                            boolean crossedBorder, String locationName) {
        this.id = id;
        this.studentId = studentId;
        this.date = date;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.crossedBorder = crossedBorder;
        this.locationName = locationName;
    }
}
