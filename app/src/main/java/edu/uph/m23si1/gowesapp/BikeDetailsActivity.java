package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class BikeDetailsActivity extends AppCompatActivity {

    // Bug 7 Fix: Mock data for our 4 bikes
    private static class BikeData {
        String name;
        int imageResId;
        BikeData(String name, int imageResId) {
            this.name = name;
            this.imageResId = imageResId;
        }
    }
    private Map<String, BikeData> bikeDatabase = new HashMap<>();

    private TextView tvBikeName;
    private ImageView ivBikeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_details);

        // Bug 7 Fix: Initialize the bike database
        initBikeDatabase();

        tvBikeName = findViewById(R.id.tv_bike_name);
        ivBikeImage = findViewById(R.id.iv_bike_image);
        Button continueButton = findViewById(R.id.btn_continue);
        ImageView backButton = findViewById(R.id.iv_back);

        // Get the "scanned" bike ID from the Intent
        String bikeId = getIntent().getStringExtra("BIKE_ID");
        if (bikeId == null) {
            bikeId = "BIKE-001"; // Default fallback
        }

        // Bug 7 Fix: Update the UI with the bike's data
        setBikeData(bikeId);

        String finalBikeId = bikeId; // For use in lambda
        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(BikeDetailsActivity.this, ConfirmRideActivity.class);
            intent.putExtra("BIKE_ID", finalBikeId);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void initBikeDatabase() {
        // You must create these drawable files: bike_001.png, bike_002.png, etc.
        bikeDatabase.put("BIKE-001", new BikeData("Gowes Cruiser", R.drawable.img_bike_placeholder));
        bikeDatabase.put("BIKE-002", new BikeData("Gowes Speedster", R.drawable.img_bike_placeholder));
        bikeDatabase.put("BIKE-003", new BikeData("Gowes City", R.drawable.img_bike_placeholder));
        bikeDatabase.put("BIKE-004", new BikeData("Gowes Mountain", R.drawable.img_bike_placeholder));
    }

    private void setBikeData(String bikeId) {
        BikeData data = bikeDatabase.get(bikeId);

        if (data != null) {
            tvBikeName.setText(data.name + " (" + bikeId + ")");
            ivBikeImage.setImageResource(data.imageResId);
        } else {
            // Handle case where QR code is for an unknown bike
            tvBikeName.setText("Unknown Bike (" + bikeId + ")");
            ivBikeImage.setImageResource(R.drawable.img_bike_placeholder);
            Toast.makeText(this, "Unknown Bike ID scanned", Toast.LENGTH_SHORT).show();
        }
    }
}