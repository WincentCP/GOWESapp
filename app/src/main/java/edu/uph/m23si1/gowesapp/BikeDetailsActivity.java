package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class BikeDetailsActivity extends AppCompatActivity {

    private static final String TAG = "BikeDetailsActivity";
    private static final String QR_PREFIX = "GOWES-";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_details);

        ImageView ivBack = findViewById(R.id.iv_back);
        TextView tvBikeName = findViewById(R.id.tv_bike_name);
        // ⬇️ This MUST match the id in activity_bike_details.xml
        MaterialButton btnContinue = findViewById(R.id.btn_continue);

        // Get bike ID / QR value from Intent
        String rawBikeId = getIntent().getStringExtra("BIKE_ID");
        Log.d(TAG, "Received BIKE_ID extra: " + rawBikeId);

        if (rawBikeId == null || rawBikeId.isEmpty()) {
            rawBikeId = "BK-001"; // fallback
        }

        // Normalize:
        //  - "GOWES-BK-001" -> "BK-001"
        //  - "BK-001" stays "BK-001"
        String normalizedBikeId;
        if (rawBikeId.startsWith(QR_PREFIX)) {
            normalizedBikeId = rawBikeId.substring(QR_PREFIX.length());
        } else {
            normalizedBikeId = rawBikeId;
        }

        if (tvBikeName != null) {
            tvBikeName.setText(normalizedBikeId);
        } else {
            Log.w(TAG, "tv_bike_name is null – check activity_bike_details.xml");
        }

        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                Log.d(TAG, "Continue button clicked, going to ConfirmRideActivity with BIKE_ID = " + normalizedBikeId);
                Toast.makeText(
                        BikeDetailsActivity.this,
                        "Continuing booking for " + normalizedBikeId,
                        Toast.LENGTH_SHORT
                ).show();

                try {
                    Intent intent = new Intent(BikeDetailsActivity.this, ConfirmRideActivity.class);
                    intent.putExtra("BIKE_ID", normalizedBikeId);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open ConfirmRideActivity", e);
                    Toast.makeText(
                            BikeDetailsActivity.this,
                            "Cannot open booking screen: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        } else {
            Log.e(TAG, "btn_continue is null. Check the button id in activity_bike_details.xml");
            Toast.makeText(this, "Internal error: continue button not found", Toast.LENGTH_SHORT).show();
        }
    }
}