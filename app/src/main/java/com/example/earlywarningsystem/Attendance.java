package com.example.earlywarningsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Attendance extends AppCompatActivity implements LocationListener {

    private static final int PERMISSION_REQUEST_LOCATION = 100;
    private static final long LOCATION_UPDATE_MIN_TIME_MS = 1000; // 1s
    private static final float LOCATION_UPDATE_MIN_DISTANCE_M = 1f; // 1 meter
    private static final long GPS_FIX_TIMEOUT_MS = 15000; // 15s fallback message

    private LocationManager locationManager;
    private Spinner spinnerStudent;
    private RadioGroup rgAttendance;
    private CheckBox cbCrossedBorder;
    private TextView tvLocation;
    private Button btnSubmit;
    private TextView tvLocationAddress;

    private double currentLatitude = Double.NaN;
    private double currentLongitude = Double.NaN;

    private List<String> studentIds = new ArrayList<>();
    private List<String> studentNames = new ArrayList<>();
    private List<String> parentPhones = new ArrayList<>();
    private List<String> studentClasses = new ArrayList<>();

    private final Handler handler = new Handler();
    private final Runnable gpsTimeoutRunnable = () -> {
        if (Double.isNaN(currentLatitude) || Double.isNaN(currentLongitude)) {
            tvLocation.setText("No GPS fix yet — move outdoors or enable location services");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        initViews();
        loadStudents();

        // Request runtime permissions if needed, then start location tracking
        if (!hasLocationPermissions()) {
            requestLocationPermissions();
        } else {
            setupLocationTracking();
        }

        btnSubmit.setOnClickListener(v -> submitAttendance());
    }

    private void initViews() {
        spinnerStudent = findViewById(R.id.spinnerStudent);
        rgAttendance = findViewById(R.id.rgAttendance);
        cbCrossedBorder = findViewById(R.id.cbCrossedBorder);
        tvLocation = findViewById(R.id.tvLocation);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvLocationAddress = findViewById(R.id.tvLocationAddress);

        // Initial message
        tvLocation.setText("Acquiring GPS location...");
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                PERMISSION_REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            boolean granted = true;
            if (grantResults.length == 0) granted = false;
            else {
                for (int res : grantResults) {
                    if (res != PackageManager.PERMISSION_GRANTED) {
                        granted = false;
                        break;
                    }
                }
            }

            if (granted) {
                setupLocationTracking();
            } else {
                tvLocation.setText("Location permission denied. Enable location to capture GPS coordinates.");
                Toast.makeText(this, "Location permission is required for GPS features", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadStudents() {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance("https://earlywarningsystem-a36d5-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                studentIds.clear();
                studentNames.clear();
                parentPhones.clear();
                studentClasses.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    String parentPhone = child.child("parentPhone").getValue(String.class);
                    String studentClass = child.child("studentClass").getValue(String.class);

                    if (id == null) continue;
                    studentIds.add(id);
                    studentNames.add((name != null ? name : "Unknown") + " (" + (studentClass != null ? studentClass : "") + ")");
                    parentPhones.add(parentPhone != null ? parentPhone : "");
                    studentClasses.add(studentClass != null ? studentClass : "");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(Attendance.this,
                        android.R.layout.simple_spinner_item, studentNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerStudent.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Attendance.this, "Failed to load students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLocationTracking() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!hasLocationPermissions()) {
            tvLocation.setText("Location permission not granted");
            return;
        }

        try {
            // Request updates from GPS provider
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_MIN_TIME_MS,
                    LOCATION_UPDATE_MIN_DISTANCE_M,
                    this);

            // Also request network provider for faster coarse fixes (if available)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        LOCATION_UPDATE_MIN_TIME_MS,
                        LOCATION_UPDATE_MIN_DISTANCE_M,
                        this);
            }

            // Try last known location (GPS first, then network)
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnown != null) {
                currentLatitude = lastKnown.getLatitude();
                currentLongitude = lastKnown.getLongitude();
                updateLocationDisplay();
            } else {
                // No last known location — show acquiring message and schedule fallback
                tvLocation.setText("Acquiring GPS location...");
                handler.postDelayed(gpsTimeoutRunnable, GPS_FIX_TIMEOUT_MS);
            }
        } catch (SecurityException se) {
            tvLocation.setText("Location permission missing");
        } catch (Exception e) {
            tvLocation.setText("Unable to start location updates: " + e.getMessage());
        }
    }

    private void updateLocationDisplay() {
        if (!Double.isNaN(currentLatitude) && !Double.isNaN(currentLongitude)) {
            String coords = String.format(Locale.getDefault(),
                    "Lat: %.5f, Lon: %.5f", currentLatitude, currentLongitude);
            tvLocation.setText(coords);

            // Try Geocoder for human-readable address
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);

                    // Build a string with multiple levels
                    StringBuilder placeName = new StringBuilder();

                    if (addr.getAdminArea() != null) { // County
                        placeName.append(addr.getAdminArea());
                    }
                    if (addr.getSubAdminArea() != null) { // Sub-county / Constituency
                        placeName.append(" | ").append(addr.getSubAdminArea());
                    }
                    if (addr.getLocality() != null) { // Town/Center
                        placeName.append(" | ").append(addr.getLocality());
                    }
                    if (addr.getSubLocality() != null) { // Smaller centers/wards
                        placeName.append(" | ").append(addr.getSubLocality());
                    }

                    tvLocationAddress.setText(placeName.toString());
                } else {
                    tvLocationAddress.setText("Unable to resolve place name");
                }
            } catch (Exception e) {
                tvLocationAddress.setText("Error resolving location: " + e.getMessage());
            }


            boolean nearBorder = isNearBorder(currentLatitude, currentLongitude);
            cbCrossedBorder.setEnabled(nearBorder);
            cbCrossedBorder.setText(nearBorder ?
                    "Student crossed border (detected near border zone)" :
                    "Student crossed border");
        } else {
            tvLocation.setText("No GPS fix yet — move outdoors or enable location services");
            tvLocationAddress.setText("No address available");
            cbCrossedBorder.setEnabled(false);
        }
    }

    private boolean isNearBorder(double lat, double lon) {
        double busiaLat = 0.4652, busiaLon = 34.1128;
        double malabaLat = 0.6369, malabaLon = 34.2736;

        double distanceToBusia = Math.sqrt(Math.pow(lat - busiaLat, 2) + Math.pow(lon - busiaLon, 2));
        double distanceToMalaba = Math.sqrt(Math.pow(lat - malabaLat, 2) + Math.pow(lon - malabaLon, 2));

        return distanceToBusia < 0.1 || distanceToMalaba < 0.1;
    }

    private void submitAttendance() {
        if (studentIds.isEmpty()) {
            Toast.makeText(this, "No students available", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spinnerStudent.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= studentIds.size()) {
            Toast.makeText(this, "Please select a student", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = studentIds.get(selectedPosition);
        String studentName = studentNames.get(selectedPosition);
        String parentPhone = parentPhones.get(selectedPosition);
        String studentClass = studentClasses.get(selectedPosition);
        String locationName = tvLocationAddress.getText().toString();

        int selectedRadioId = rgAttendance.getCheckedRadioButtonId();
        if (selectedRadioId == -1) {
            Toast.makeText(this, "Please select attendance status", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadio = findViewById(selectedRadioId);
        String status = selectedRadio.getText().toString();
        boolean crossedBorder = cbCrossedBorder.isChecked();

        // If no GPS fix, still allow submission but warn user
        if (Double.isNaN(currentLatitude) || Double.isNaN(currentLongitude)) {
            Toast.makeText(this, "No GPS fix yet — attendance will be saved without coordinates", Toast.LENGTH_LONG).show();
        }

        DatabaseReference attendanceRef = FirebaseDatabase.getInstance("https://earlywarningsystem-a36d5-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Attendance");
        String attId = attendanceRef.push().getKey();

        AttendanceRecord record = new AttendanceRecord(attId, studentId, getCurrentDate(),
                status,
                Double.isNaN(currentLatitude) ? 0.0 : currentLatitude,
                Double.isNaN(currentLongitude) ? 0.0 : currentLongitude,
                crossedBorder,
                locationName );

        attendanceRef.child(attId).setValue(record).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Attendance marked for " + studentName, Toast.LENGTH_SHORT).show();

                if ("Absent".equalsIgnoreCase(status) && parentPhone != null && !parentPhone.isEmpty()) {
                    sendAbsenceAlert(studentName, studentClass, parentPhone);
                }

                rgAttendance.clearCheck();
                cbCrossedBorder.setChecked(false);
            } else {
                Toast.makeText(this, "Failed to mark attendance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendAbsenceAlert(String studentName, String studentClass, String parentPhone) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                String message = "ALERT: Your child " + studentName + " (" + studentClass + ") was marked absent today.";
                smsManager.sendTextMessage(parentPhone, null, message, null, null);
                Toast.makeText(this, "SMS sent to parent", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Optionally request SEND_SMS permission here if you want to send SMS at runtime
            Toast.makeText(this, "SMS permission not granted. Cannot send alert.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
            updateLocationDisplay();
        }
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(gpsTimeoutRunnable);
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException ignored) {}
        }
    }

    // Firebase model class
    public static class AttendanceRecord {
        public String id, studentId, date, status;
        public double latitude, longitude;
        public boolean crossedBorder;
        public String locationName; // NEW

        public AttendanceRecord() {} // Needed for Firebase

        public AttendanceRecord(String id, String studentId, String date, String status,
                                double latitude, double longitude, boolean crossedBorder,
                                String locationName) {
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



}
