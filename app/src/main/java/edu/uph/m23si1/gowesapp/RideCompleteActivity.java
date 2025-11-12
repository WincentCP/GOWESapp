package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class RideCompleteActivity extends AppCompatActivity {

    // (Perbaikan Bug 12) Konstanta untuk perhitungan CO2
    private static final double AVG_BIKE_SPEED_KMH = 15.0; // Kecepatan rata-rata sepeda
    private static final double CO2_GRAMS_PER_KM_CAR = 120.0; // Rata-rata CO2 mobil

    private TextView tvRideDuration, tvAmountPaid, tvThankYou, tvCo2Saved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_complete);

        tvRideDuration = findViewById(R.id.tv_ride_duration);
        tvAmountPaid = findViewById(R.id.tv_amount_paid);
        tvThankYou = findViewById(R.id.tv_subtitle);
        tvCo2Saved = findViewById(R.id.tv_co2_saved); // Pastikan ID ini ada di XML

        // Ambil data dari Intent
        String duration = getIntent().getStringExtra("RIDE_DURATION");
        double cost = getIntent().getDoubleExtra("FINAL_COST", 0.0);
        long totalSeconds = getIntent().getLongExtra("TOTAL_SECONDS", 0);

        // Isi detail perjalanan
        tvRideDuration.setText(duration);
        tvAmountPaid.setText(formatCurrency(cost));

        // (Perbaikan Bug 12) Hitung dan tampilkan CO2 yang dihemat
        double totalHours = totalSeconds / 3600.0;
        double distanceKm = totalHours * AVG_BIKE_SPEED_KMH;
        double co2SavedGrams = distanceKm * CO2_GRAMS_PER_KM_CAR;

        // Format ke "10g CO2" atau "1.2kg CO2"
        String co2SavedText;
        if (co2SavedGrams < 1000) {
            co2SavedText = String.format(Locale.getDefault(), "%.0fg CO₂", co2SavedGrams);
        } else {
            co2SavedText = String.format(Locale.getDefault(), "%.1fkg CO₂", co2SavedGrams / 1000.0);
        }
        tvCo2Saved.setText(co2SavedText);


        // --- Tombol untuk kembali ke Home ---
        Button backHomeButton = findViewById(R.id.btn_back_home);
        backHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(RideCompleteActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private String formatCurrency(double amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0);
        return currencyFormatter.format(amount);
    }
}