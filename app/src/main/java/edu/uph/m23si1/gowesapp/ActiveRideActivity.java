package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.NumberFormat;
import java.util.Locale;

public class ActiveRideActivity extends AppCompatActivity {

    // Define the rate: 8000 Rupiah per 60 minutes
    // This is 8000 / (60 * 60) = 2.22... Rupiah per second
    private static final double RATE_PER_SECOND = 8000.0 / 3600.0;

    private TextView tvTimer, tvCurrentCost; // Added tvCurrentCost
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0;
    private long timeInMilliseconds = 0;

    // To store the final values
    private String finalRideDuration;
    private double finalCalculatedCost = 0.0;

    // Runnable that updates the timer
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            timeInMilliseconds = System.currentTimeMillis() - startTime;

            // 1. Update Timer UI
            updateTimerUI(timeInMilliseconds);

            // 2. Calculate and Update Cost UI
            finalCalculatedCost = (timeInMilliseconds / 1000.0) * RATE_PER_SECOND;
            updateCostUI(finalCalculatedCost);

            timerHandler.postDelayed(this, 1000); // Run again in 1 second
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_ride);

        tvTimer = findViewById(R.id.tv_timer);
        tvCurrentCost = findViewById(R.id.tv_current_cost); // Find the cost TextView
        Button parkButton = findViewById(R.id.btn_park);
        Button backHomeButton = findViewById(R.id.btn_back_home);

        parkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the timer
                timerHandler.removeCallbacks(timerRunnable);

                // Save the final values
                finalRideDuration = tvTimer.getText().toString();
                // The cost is already up-to-date in 'finalCalculatedCost'

                // Show the "Detecting Bike" dialog
                showDetectingDialog();
            }
        });

        backHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to main activity
                finish();
            }
        });

        // Start the timer when the activity is created
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void updateTimerUI(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateCostUI(double cost) {
        // Format the double as Rupiah currency
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0); // No decimals

        tvCurrentCost.setText(currencyFormatter.format(cost));
    }

    private void showDetectingDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_detecting_bike);
        dialog.setCancelable(false); // User can't dismiss by swiping

        Button cancelRideButton = dialog.findViewById(R.id.btn_cancel);
        cancelRideButton.setOnClickListener(v -> {
            dialog.dismiss();
            // User cancelled parking, so restart the timer
            startTime = System.currentTimeMillis() - timeInMilliseconds; // Resume from where it left off
            timerHandler.postDelayed(timerRunnable, 0);
        });

        dialog.show();

        // --- Simulate bike detection ---
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if(dialog.isShowing()){
                dialog.dismiss();
                showBikeDetectedDialog();
            }
        }, 3000);
    }

    private void showBikeDetectedDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_bike_detected);
        dialog.setCancelable(false);

        Button takePhoto = dialog.findViewById(R.id.btn_take_photo);
        takePhoto.setOnClickListener(v -> {
            // --- Add logic to open camera for photo ---

            // After photo, go to Ride Complete
            dialog.dismiss();
            Intent intent = new Intent(ActiveRideActivity.this, RideCompleteActivity.class);

            // Pass the REAL calculated data to the invoice screen
            intent.putExtra("RIDE_DURATION", finalRideDuration);
            intent.putExtra("FINAL_COST", finalCalculatedCost); // Pass the double

            startActivity(intent);
            finish(); // Finish this activity
        });

        dialog.show();
    }

    // You would also call this dialog if detection fails
    private void showDetectionFailedDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_detection_failed);
        // ... set click listeners for "Try Again", "Report Issue", etc. ...
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the timer handler when the activity is destroyed
        timerHandler.removeCallbacks(timerRunnable);
    }
}