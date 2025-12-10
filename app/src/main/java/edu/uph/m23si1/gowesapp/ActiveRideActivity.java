package edu.uph.m23si1.gowesapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ActiveRideActivity extends AppCompatActivity {

    private static final double RATE_PER_BLOCK = 8000.0;
    private static final double SECONDS_PER_BLOCK = 3600.0;

    private TextView tvTimer, tvCurrentCost, tvBikeName;
    private Handler timerHandler;
    private Runnable timerRunnable;

    private long startTime = 0;
    private long timeInMilliseconds = 0;
    private String bikeModel = "Gowes Bike";
    private String bikeId = "BK-UNKNOWN";
    private String slotKey;

    private double finalCalculatedCost = 0.0;
    private String finalRideDuration;

    private FirebaseFirestore db;
    private DatabaseReference rtDbRef;
    private String userId;
    private String paymentMethod = "Wallet";
    private String currentRideDocId;

    private final ActivityResultLauncher<Intent> cameraResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            endRide();
                        } else {
                            resumeTimerAfterCancel();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_ride);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        userId = user.getUid();

        tvTimer = findViewById(R.id.tv_timer);
        tvCurrentCost = findViewById(R.id.tv_current_cost);
        tvBikeName = findViewById(R.id.tv_bike_name);
        Button parkButton = findViewById(R.id.btn_park);
        Button backHomeButton = findViewById(R.id.btn_back_home);

        boolean isNewRide = getIntent().getBooleanExtra("IS_NEW_RIDE", false);

        if (isNewRide) {
            initializeNewRide();
        } else {
            resumeRideState();
        }

        parkButton.setOnClickListener(v -> {
            timerHandler.removeCallbacks(timerRunnable);
            finalRideDuration = tvTimer.getText().toString();
            if (slotKey != null) {
                updateServo(slotKey, "OPEN");
                Toast.makeText(this, "Station Unlocked! Please insert bike.", Toast.LENGTH_SHORT).show();
            }
            showDetectingDialog();
        });

        backHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ActiveRideActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void updateServo(String slot, String status) {
        DatabaseReference slotRef = FirebaseDatabase.getInstance("https://smartbike-c6082-default-rtdb.firebaseio.com/")
                .getReference("stations/station_uph_medan/slots")
                .child(slot);
        slotRef.child("servoStatus").setValue(status);
    }

    private void initializeNewRide() {
        paymentMethod = getIntent().getStringExtra(RideCompleteActivity.EXTRA_PAYMENT_METHOD);
        if (paymentMethod == null) paymentMethod = "Wallet";

        String intentModel = getIntent().getStringExtra("BIKE_MODEL");
        if (intentModel != null) bikeModel = intentModel;

        bikeId = getIntent().getStringExtra("BIKE_ID");
        if (bikeId == null) bikeId = bikeModel;

        slotKey = getIntent().getStringExtra("SLOT_KEY");

        if(tvBikeName != null) tvBikeName.setText(bikeModel);

        startTime = System.currentTimeMillis();
        currentRideDocId = "ride_" + startTime;

        Map<String, Object> rideData = new HashMap<>();
        rideData.put("rideId", currentRideDocId);
        rideData.put("userId", userId);
        rideData.put("bikeId", bikeId);
        rideData.put("slotKey", slotKey);
        rideData.put("status", "Active");
        rideData.put("startTime", startTime);
        rideData.put("startStation", "UPH Medan Station");
        rideData.put("initialCost", 0);
        rideData.put("paymentMethod", paymentMethod);

        db.collection("rides").document(currentRideDocId).set(rideData);

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("isActiveRide", true);
        userUpdates.put("currentRideId", currentRideDocId);

        db.collection("users").document(userId).set(userUpdates, SetOptions.merge());

        rtDbRef = FirebaseDatabase.getInstance("https://smartbike-c6082-default-rtdb.firebaseio.com/").getReference("activeRides").child(userId);
        rtDbRef.child("isActive").setValue(true);
        rtDbRef.child("startTime").setValue(startTime);
        rtDbRef.child("bikeModel").setValue(bikeModel);
        if (slotKey != null) rtDbRef.child("slotKey").setValue(slotKey);

        saveLocalState();
        startTimer();
    }

    private void resumeRideState() {
        SharedPreferences prefs = getSharedPreferences("GowesAppPrefs", Context.MODE_PRIVATE);
        startTime = prefs.getLong("ride_start_time", 0);
        bikeModel = prefs.getString("active_bike_model", "Gowes Electric Bike");
        paymentMethod = prefs.getString("active_payment_method", "Wallet");
        slotKey = prefs.getString("active_slot_key", null);

        if (startTime == 0) {
            db.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getBoolean("isActiveRide"))) {
                    currentRideDocId = snapshot.getString("currentRideId");
                    if (currentRideDocId != null) {
                        db.collection("rides").document(currentRideDocId).get().addOnSuccessListener(rideSnap -> {
                            if (rideSnap.exists()) {
                                Long start = rideSnap.getLong("startTime");
                                if (start != null) startTime = start;

                                String bId = rideSnap.getString("bikeId");
                                if (bId != null) {
                                    bikeId = bId;
                                    // Assuming model is same as ID for simplicity or fetch properly
                                    bikeModel = bId;
                                    if(tvBikeName != null) tvBikeName.setText(bikeModel);
                                }
                                startTimer();
                            }
                        });
                    }
                } else {
                    finish();
                }
            });
        } else {
            if(tvBikeName != null) tvBikeName.setText(bikeModel);
            startTimer();
        }
    }

    private void saveLocalState() {
        SharedPreferences prefs = getSharedPreferences("GowesAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("ride_start_time", startTime);
        editor.putString("active_bike_model", bikeModel);
        editor.putString("active_payment_method", paymentMethod);
        editor.putString("active_slot_key", slotKey);
        editor.apply();
    }

    private void startTimer() {
        if (timerHandler == null) timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timeInMilliseconds = System.currentTimeMillis() - startTime;
                if (timeInMilliseconds < 0) timeInMilliseconds = 0;

                long totalSeconds = timeInMilliseconds / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));

                long totalBlocks = (long) Math.ceil(totalSeconds / SECONDS_PER_BLOCK);
                if (totalBlocks == 0) totalBlocks = 1;
                finalCalculatedCost = totalBlocks * RATE_PER_BLOCK;
                tvCurrentCost.setText(String.format(Locale.getDefault(), "Rp %.0f", finalCalculatedCost));

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void showDetectingDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_detecting_bike);
        dialog.setCancelable(false);
        if (dialog.findViewById(R.id.btn_cancel) != null) {
            dialog.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
                dialog.dismiss();
                resumeTimerAfterCancel();
            });
        }
        dialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                if (slotKey != null) {
                    updateServo(slotKey, "LOCKED");
                    Toast.makeText(this, "Bike Locked Successfully!", Toast.LENGTH_SHORT).show();
                }
                showBikeDetectedDialog();
            }
        }, 3000);
    }

    private void showBikeDetectedDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_bike_detected);
        dialog.setCancelable(false);
        if (dialog.findViewById(R.id.btn_take_photo) != null) {
            dialog.findViewById(R.id.btn_take_photo).setOnClickListener(v -> {
                dialog.dismiss();
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraResultLauncher.launch(cameraIntent);
            });
        }
        dialog.show();
    }

    private void resumeTimerAfterCancel() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    private void endRide() {
        if (slotKey != null && !slotKey.isEmpty()) {
            DatabaseReference slotRef = FirebaseDatabase.getInstance("https://smartbike-c6082-default-rtdb.firebaseio.com/")
                    .getReference("stations/station_uph_medan/slots")
                    .child(slotKey);
            Map<String, Object> updates = new HashMap<>();
            updates.put("servoStatus", "LOCKED");
            updates.put("status", "Available");
            slotRef.updateChildren(updates);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("isActiveRide", false);
        updates.put("currentRideId", null);
        db.collection("users").document(userId).update(updates);

        if (currentRideDocId == null) currentRideDocId = "ride_" + startTime;

        long durationMs = timeInMilliseconds;
        int baseCharge = (int) Math.round(finalCalculatedCost);
        double co2Grams = (durationMs / 60000.0) * (1.1 / 45.0) * 1000.0;

        if (rtDbRef == null) {
            rtDbRef = FirebaseDatabase.getInstance("https://smartbike-c6082-default-rtdb.firebaseio.com/")
                    .getReference("activeRides").child(userId);
        }
        rtDbRef.removeValue();

        SharedPreferences prefs = getSharedPreferences("GowesAppPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(ActiveRideActivity.this, RideCompleteActivity.class);
        intent.putExtra(RideCompleteActivity.EXTRA_RIDE_ID, currentRideDocId); // PASS RIDE ID
        intent.putExtra(RideCompleteActivity.EXTRA_RIDE_DURATION_MS, durationMs);
        intent.putExtra(RideCompleteActivity.EXTRA_BASE_CHARGE, baseCharge);
        intent.putExtra(RideCompleteActivity.EXTRA_PAYMENT_METHOD, paymentMethod);
        intent.putExtra(RideCompleteActivity.EXTRA_CO2_SAVED, co2Grams);
        intent.putExtra(RideCompleteActivity.EXTRA_BIKE_ID, bikeModel);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}