package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ActiveRideActivity extends AppCompatActivity {

    private static final String TAG = "ActiveRideActivity";

    // Tarif per blok
    private static final double RATE_PER_BLOCK = 8000.0;
    private static final double SECONDS_PER_BLOCK = 3600.0; // 60 menit

    private TextView tvTimer, tvCurrentCost;
    private Handler timerHandler;
    private Runnable timerRunnable;

    private long startTime = 0;
    private long timeInMilliseconds = 0;

    private String finalRideDuration;
    private double finalCalculatedCost = 0.0; // base charge before discount

    private FirebaseFirestore db;
    private DatabaseReference rtDbRef; // Realtime Database untuk data live
    private String userId;

    // Payment method passed from ConfirmRideActivity ("Wallet" / "Card")
    private String paymentMethod = "Wallet";

    // Ambil foto untuk konfirmasi parkir
    private final ActivityResultLauncher<Intent> cameraResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
                            endRide();
                        } else {
                            Toast.makeText(this, "Photo cancelled. Please take photo to end ride.", Toast.LENGTH_SHORT).show();
                            // Kalau batal, lanjutkan timer lagi
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

        // Baca payment method dari ConfirmRideActivity
        String pm = getIntent().getStringExtra(RideCompleteActivity.EXTRA_PAYMENT_METHOD);
        if (pm != null && !pm.isEmpty()) {
            paymentMethod = pm;
        }

        // Realtime Database untuk data live
        rtDbRef = FirebaseDatabase.getInstance().getReference("activeRides").child(userId);

        // Set perjalanan aktif di Firestore & Realtime DB
        db.collection("users").document(userId).update("isActiveRide", true);
        rtDbRef.child("isActive").setValue(true);
        rtDbRef.child("startTime").setValue(System.currentTimeMillis());

        tvTimer = findViewById(R.id.tv_timer);
        tvCurrentCost = findViewById(R.id.tv_current_cost);
        Button parkButton = findViewById(R.id.btn_park);
        Button backHomeButton = findViewById(R.id.btn_back_home);

        // Inisialisasi timer handler & runnable
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timeInMilliseconds = System.currentTimeMillis() - startTime;
                long totalSeconds = timeInMilliseconds / 1000;
                updateTimerUI(totalSeconds);

                // Hitung cost per blok (minimal 1 blok)
                long totalBlocks = (long) Math.ceil(totalSeconds / SECONDS_PER_BLOCK);
                if (totalBlocks == 0) totalBlocks = 1;
                finalCalculatedCost = totalBlocks * RATE_PER_BLOCK;
                updateCostUI(finalCalculatedCost);

                // Update Realtime Database
                if (rtDbRef != null) {
                    rtDbRef.child("currentTime").setValue(totalSeconds);
                    rtDbRef.child("currentCost").setValue(finalCalculatedCost);
                }

                timerHandler.postDelayed(this, 1000);
            }
        };

        parkButton.setOnClickListener(v -> {
            timerHandler.removeCallbacks(timerRunnable);
            finalRideDuration = tvTimer.getText().toString();
            showDetectingDialog();
        });

        backHomeButton.setOnClickListener(v -> finish());

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void resumeTimerAfterCancel() {
        if (timerHandler == null || timerRunnable == null) return;
        // lanjutkan dari waktu terakhir
        startTime = System.currentTimeMillis() - timeInMilliseconds;
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void updateTimerUI(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateCostUI(double cost) {
        tvCurrentCost.setText(formatCurrency(cost));
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

    private void endRide() {
        // Tandai ride tidak aktif
        db.collection("users").document(userId).update("isActiveRide", false);
        if (rtDbRef != null) {
            rtDbRef.removeValue(); // Hapus data live
        }

        long durationMs = timeInMilliseconds;
        long totalSeconds = durationMs / 1000;

        // Base charge dari perhitungan blok
        int baseCharge = (int) Math.round(finalCalculatedCost);

        // Diskon -5% jika Wallet
        double discountRate = paymentMethod.equalsIgnoreCase("Wallet") ? 0.05 : 0.0;
        int discountAmount = (int) Math.round(baseCharge * discountRate);
        int finalCharge = baseCharge - discountAmount;

        // Hitung CO2 dalam gram (45 menit ≈ 1.1kg)
        double minutes = durationMs / 60000.0;
        double kgPerMinute = 1.1 / 45.0;
        double co2Kg = minutes * kgPerMinute;
        double co2Grams = co2Kg * 1000.0; // sekarang dalam gram

        // Kurangi saldo dompet hanya jika bayar pakai Wallet
        if (paymentMethod.equalsIgnoreCase("Wallet")) {
            DocumentReference userDocRef = db.collection("users").document(userId);
            db.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(userDocRef);
                Number currentBalanceNum = (Number) snapshot.get("walletBalance");
                int currentBalance = (currentBalanceNum != null) ? currentBalanceNum.intValue() : 0;
                int newBalance = currentBalance - finalCharge;
                transaction.update(userDocRef, "walletBalance", newBalance);
                return null;
            }).addOnFailureListener(e -> Log.w(TAG, "Gagal mengurangi saldo", e));
        }

        // Simpan riwayat perjalanan
        Map<String, Object> rideHistory = new HashMap<>();
        rideHistory.put("duration", finalRideDuration);
        rideHistory.put("durationSeconds", totalSeconds);
        rideHistory.put("baseCost", baseCharge);
        rideHistory.put("finalCost", finalCharge);
        rideHistory.put("paymentMethod", paymentMethod);
        rideHistory.put("co2SavedGrams", co2Grams);
        rideHistory.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(userId).collection("rideHistory")
                .add(rideHistory)
                .addOnSuccessListener(docRef -> Log.d(TAG, "Riwayat perjalanan disimpan"))
                .addOnFailureListener(e -> Log.w(TAG, "Gagal simpan riwayat", e));

        // Kirim data ke RideCompleteActivity
        Intent intent = new Intent(ActiveRideActivity.this, RideCompleteActivity.class);
        intent.putExtra(RideCompleteActivity.EXTRA_RIDE_DURATION_MS, durationMs);
        intent.putExtra(RideCompleteActivity.EXTRA_BASE_CHARGE, baseCharge);
        intent.putExtra(RideCompleteActivity.EXTRA_PAYMENT_METHOD, paymentMethod);
        intent.putExtra(RideCompleteActivity.EXTRA_CO2_SAVED, co2Grams); // dalam gram
        startActivity(intent);
        finish();
    }

    private String formatCurrency(double amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0);
        return currencyFormatter.format(amount);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}