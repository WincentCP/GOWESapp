package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class RideCompleteActivity extends AppCompatActivity {

    private TextView tvSubtitle;
    private TextView tvRideDuration, tvAmountPaid, tvRideCharge, tvPaymentMethod, tvCo2Saved;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Keys for Intent extras
    public static final String EXTRA_RIDE_DURATION_MS   = "EXTRA_RIDE_DURATION_MS";   // long (ms)
    public static final String EXTRA_BASE_CHARGE        = "EXTRA_BASE_CHARGE";        // int (rupiah, before discount)
    public static final String EXTRA_PAYMENT_METHOD     = "EXTRA_PAYMENT_METHOD";     // String ("Wallet" / "Card")
    public static final String EXTRA_CO2_SAVED          = "EXTRA_CO2_SAVED";          // double (GRAMS)

    // base pricing default = 8.000
    private static final int DEFAULT_BASE_CHARGE = 8000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_complete);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        tvSubtitle      = findViewById(R.id.tv_subtitle);
        tvRideDuration  = findViewById(R.id.tv_ride_duration);
        tvAmountPaid    = findViewById(R.id.tv_amount_paid);
        tvRideCharge    = findViewById(R.id.tv_ride_charge);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);
        tvCo2Saved      = findViewById(R.id.tv_co2_saved);
        Button btnBackHome = findViewById(R.id.btn_back_home);

        // Subtitle nama user
        fetchUserName();

        // Isi data perjalanan dari Intent
        populateRideDataFromIntent();

        if (btnBackHome != null) {
            btnBackHome.setOnClickListener(v -> {
                Intent intent = new Intent(RideCompleteActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void fetchUserName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DocumentReference userDoc = db.collection("users").document(user.getUid());
            userDoc.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String fullName = documentSnapshot.getString("fullName");
                    if (fullName != null && !fullName.isEmpty()) {
                        String firstName = fullName.split(" ")[0];
                        tvSubtitle.setText("Thank you, " + firstName + "! Stay Safe");
                    } else {
                        tvSubtitle.setText("Thank you! Stay Safe");
                    }
                }
            }).addOnFailureListener(e -> tvSubtitle.setText("Thank you! Stay Safe"));
        } else {
            tvSubtitle.setText("Thank you! Stay Safe");
        }
    }

    private void populateRideDataFromIntent() {
        Intent intent = getIntent();

        // 1️⃣ Durasi
        long durationMs = intent.getLongExtra(EXTRA_RIDE_DURATION_MS, -1L);

        // Fallback kalau belum dikirim
        if (durationMs <= 0) {
            long durationSeconds = intent.getLongExtra("TOTAL_SECONDS", -1L);
            if (durationSeconds > 0) {
                durationMs = durationSeconds * 1000L;
            } else {
                durationMs = 0L;
            }
        }

        if (tvRideDuration != null) {
            tvRideDuration.setText(formatDuration(durationMs));
        }

        // 2️⃣ Base charge (before discount)
        int baseCharge = intent.getIntExtra(EXTRA_BASE_CHARGE, DEFAULT_BASE_CHARGE);

        // 3️⃣ Payment method
        String paymentMethod = intent.getStringExtra(EXTRA_PAYMENT_METHOD);
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            paymentMethod = "Wallet"; // default
        }

        // 4️⃣ Discount: Wallet = -5%
        double discountRate = paymentMethod.equalsIgnoreCase("Wallet") ? 0.05 : 0.0;
        int discountAmount = (int) Math.round(baseCharge * discountRate);
        int finalCharge = baseCharge - discountAmount;

        // 5️⃣ CO2 saved dalam GRAM
        double co2Grams = intent.getDoubleExtra(EXTRA_CO2_SAVED, -1.0);
        if (co2Grams < 0) {
            double minutes = durationMs / 60000.0;
            double kgPerMinute = 1.1 / 45.0;
            double co2Kg = minutes * kgPerMinute;
            co2Grams = co2Kg * 1000.0;
        }

        if (tvCo2Saved != null) {
            // tampilkan sebagai "xxxg"
            tvCo2Saved.setText(String.format(Locale.getDefault(), "%.0fg", co2Grams));
        }

        // 6️⃣ Update UI invoice
        if (tvRideCharge != null) {
            // Original charge sebelum diskon
            tvRideCharge.setText(formatCurrency(baseCharge));      // contoh: Rp 8.000
        }
        if (tvAmountPaid != null) {
            // Final amount setelah diskon
            tvAmountPaid.setText(formatCurrency(finalCharge));     // contoh: Rp 7.600
        }
        if (tvPaymentMethod != null) {
            if (discountRate > 0) {
                tvPaymentMethod.setText(paymentMethod + " (-5% discount)");
            } else {
                tvPaymentMethod.setText(paymentMethod);
            }
        }
    }

    private String formatDuration(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatCurrency(int amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0);
        return currencyFormatter.format(amount);
    }
}