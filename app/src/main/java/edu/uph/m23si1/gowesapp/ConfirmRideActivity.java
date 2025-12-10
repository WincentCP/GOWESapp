package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // Import untuk delay
import android.os.Looper;  // Import untuk delay di thread utama
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference; // Import Realtime DB
import com.google.firebase.database.FirebaseDatabase; // Import Realtime DB
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConfirmRideActivity extends AppCompatActivity {

    // UI Components
    private ImageView ivBack;
    private TextView tvBikeName, tvRate, tvWalletBalance, tvStandardRate, tvDiscountAmount;
    private CardView payAsYouGoCard, useWalletCard;
    private ImageView radioBtnPayg, radioBtnWallet;
    private TextView btnInlineTopUp;
    private MaterialButton btnConfirmRide;
    private RelativeLayout layoutDiscount;

    // Data
    private String bikeId;
    private String slotKey; // ID Slot (misal "slot_1") dari BikeDetails
    private double walletBalance = 0.0;
    private static final double HOURLY_RATE = 8000.0;
    private static final double DISCOUNT_RATE = 0.05; // 5%
    private static final int MIN_TOP_UP_AMOUNT = 10000;

    // State
    private String selectedMethod = "Wallet"; // Default

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_your_ride);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get extras
        bikeId = getIntent().getStringExtra("BIKE_ID");
        slotKey = getIntent().getStringExtra("SLOT_KEY");

        initViews();
        setupListeners();

        // Initial setup
        updateBikeInfo();
        fetchWalletBalance();
        selectPaymentMethod("Wallet"); // Set default
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        tvBikeName = findViewById(R.id.tv_bike_name);
        tvRate = findViewById(R.id.tv_rate);
        tvWalletBalance = findViewById(R.id.tv_wallet_balance);
        tvStandardRate = findViewById(R.id.tv_standard_rate);
        tvDiscountAmount = findViewById(R.id.tv_discount_amount);

        payAsYouGoCard = findViewById(R.id.pay_as_you_go_card);
        useWalletCard = findViewById(R.id.use_wallet_card);
        radioBtnPayg = findViewById(R.id.radio_btn_payg);
        radioBtnWallet = findViewById(R.id.radio_btn_wallet);

        btnInlineTopUp = findViewById(R.id.btn_inline_top_up);
        btnConfirmRide = findViewById(R.id.btn_confirm);

        layoutDiscount = findViewById(R.id.layout_discount);
    }

    private void setupListeners() {
        if(ivBack != null) ivBack.setOnClickListener(v -> finish());

        // Custom Radio Selection Logic
        if(payAsYouGoCard != null) payAsYouGoCard.setOnClickListener(v -> selectPaymentMethod("PayAsYouGo"));
        if(useWalletCard != null) useWalletCard.setOnClickListener(v -> selectPaymentMethod("Wallet"));

        if(btnConfirmRide != null) btnConfirmRide.setOnClickListener(v -> startRide());

        // Top Up Logic
        if(btnInlineTopUp != null) {
            btnInlineTopUp.setOnClickListener(v -> showTopUpDialog());
        }
    }

    private void updateBikeInfo() {
        if (tvBikeName == null) return;

        if (bikeId != null) {
            String display = bikeId;
            if (!display.startsWith("Bike") && display.startsWith("BK")) {
                display = "Bike " + display;
            }
            tvBikeName.setText(display);
        } else {
            tvBikeName.setText("Gowes Electric Bike");
        }

        if(tvStandardRate != null) tvStandardRate.setText("Rp " + formatMoney(HOURLY_RATE));
    }

    private void selectPaymentMethod(String method) {
        this.selectedMethod = method;

        if ("Wallet".equals(method)) {
            // UI Selection State: Wallet Active
            if(payAsYouGoCard != null) payAsYouGoCard.setBackgroundResource(R.drawable.card_payment_unselected);
            if(radioBtnPayg != null) radioBtnPayg.setImageResource(R.drawable.ic_radio_button_unchecked);

            if(useWalletCard != null) useWalletCard.setBackgroundResource(R.drawable.card_payment_selected);
            if(radioBtnWallet != null) radioBtnWallet.setImageResource(R.drawable.ic_radio_button_checked);

            // Pricing Breakdown Logic (With Discount)
            if(layoutDiscount != null) layoutDiscount.setVisibility(View.VISIBLE);

            double discount = HOURLY_RATE * DISCOUNT_RATE;

            if(tvDiscountAmount != null) tvDiscountAmount.setText("-Rp " + formatMoney(discount));

        } else {
            // UI Selection State: PayAsYouGo Active
            if(payAsYouGoCard != null) payAsYouGoCard.setBackgroundResource(R.drawable.card_payment_selected);
            if(radioBtnPayg != null) radioBtnPayg.setImageResource(R.drawable.ic_radio_button_checked);

            if(useWalletCard != null) useWalletCard.setBackgroundResource(R.drawable.card_payment_unselected);
            if(radioBtnWallet != null) radioBtnWallet.setImageResource(R.drawable.ic_radio_button_unchecked);

            // Pricing Breakdown Logic (No Discount)
            if(layoutDiscount != null) layoutDiscount.setVisibility(View.GONE);
        }
    }

    private void fetchWalletBalance() {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null && snapshot.exists()) {
                        Double bal = snapshot.getDouble("walletBalance");
                        walletBalance = bal != null ? bal : 0.0;
                        if (tvWalletBalance != null) tvWalletBalance.setText("Balance: Rp " + formatMoney(walletBalance));
                    }
                });
    }

    private void showTopUpDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_top_up_balance);

        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnTopUp = dialog.findViewById(R.id.btn_top_up);
        EditText etAmount = dialog.findViewById(R.id.et_amount);

        // Preset buttons logic
        View.OnClickListener presetListener = v -> {
            if (etAmount != null && v instanceof Button) {
                String text = ((Button) v).getText().toString();
                String cleanAmount = text.replaceAll("[^0-9]", "");
                etAmount.setText(cleanAmount);
                etAmount.setSelection(etAmount.getText().length());
            }
        };

        int[] presetIds = {R.id.btn_amount_30k, R.id.btn_amount_50k, R.id.btn_amount_100k, R.id.btn_amount_200k};
        for (int id : presetIds) {
            View btn = dialog.findViewById(id);
            if (btn != null) btn.setOnClickListener(presetListener);
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnTopUp != null && etAmount != null) {
            btnTopUp.setOnClickListener(v -> {
                String amountStr = etAmount.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    int amount = Integer.parseInt(amountStr);
                    if (amount < MIN_TOP_UP_AMOUNT) {
                        Toast.makeText(this, "Min top up is Rp " + formatMoney(MIN_TOP_UP_AMOUNT), Toast.LENGTH_SHORT).show();
                    } else {
                        processTopUp(amount, dialog);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            });
        }
        dialog.show();
    }

    private void processTopUp(int amountToAdd, BottomSheetDialog dialog) {
        if (mAuth.getCurrentUser() == null) return;
        DocumentReference userDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        userDocRef.update("walletBalance", FieldValue.increment(amountToAdd))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Top Up Successful!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // Add Transaction
                    Map<String, Object> txn = new HashMap<>();
                    txn.put("amount", amountToAdd);
                    txn.put("description", "Top Up Wallet");
                    txn.put("type", "TopUp");
                    txn.put("status", "Success");
                    txn.put("timestamp", System.currentTimeMillis());
                    userDocRef.collection("transactions").add(txn);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void startRide() {
        if ("Wallet".equals(selectedMethod)) {
            // Check balance (Require at least initial hold or some minimum)
            if (walletBalance < 10000) {
                Toast.makeText(this, "Insufficient balance. Please top up.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // VALIDASI SLOT KEY (Pastikan dikirim dari halaman sebelumnya)
        if (slotKey == null || slotKey.isEmpty()) {
            Toast.makeText(this, "Error: Slot ID missing. Please rescan.", Toast.LENGTH_SHORT).show();
            // slotKey = "slot_1"; // Uncomment untuk testing tanpa intent
            return;
        }

        // 1. Ubah tombol jadi loading
        btnConfirmRide.setEnabled(false);
        btnConfirmRide.setText("Unlocking Station...");

        // 2. Referensi Database (Gunakan URL spesifik Anda)
        DatabaseReference slotRef = FirebaseDatabase.getInstance("https://smartbike-c6082-default-rtdb.firebaseio.com/")
                .getReference("stations/station_uph_medan/slots")
                .child(slotKey);

        // 3. LOGIKA BUKA SERVO (STEP 1)
        // Kirim perintah "OPEN" dan ubah status jadi "InUse"
        Map<String, Object> updates = new HashMap<>();
        updates.put("servoStatus", "OPEN");
        updates.put("status", "InUse");

        slotRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            // Beritahu user stasiun terbuka
            Toast.makeText(ConfirmRideActivity.this, "Station Unlocked! Please take the bike.", Toast.LENGTH_SHORT).show();

            // 4. LOGIKA TUTUP KEMBALI (STEP 2 - Delay 3 Detik)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                // Kunci kembali servo (Reset mekanik dock)
                slotRef.child("servoStatus").setValue("LOCKED");

                // Pindah ke Halaman Timer (Active Ride)
                Intent intent = new Intent(ConfirmRideActivity.this, ActiveRideActivity.class);
                intent.putExtra("IS_NEW_RIDE", true);
                intent.putExtra("BIKE_ID", bikeId);
                if(tvBikeName != null) {
                    intent.putExtra("BIKE_MODEL", tvBikeName.getText().toString());
                }
                intent.putExtra("SLOT_KEY", slotKey); // Teruskan SlotKey ke Timer (untuk pengembalian nanti)
                intent.putExtra(RideCompleteActivity.EXTRA_PAYMENT_METHOD, selectedMethod);

                startActivity(intent);
                finish();

            }, 3000); // Delay 3000ms (3 detik)

        }).addOnFailureListener(e -> {
            // Gagal Update Database
            btnConfirmRide.setEnabled(true);
            btnConfirmRide.setText("Confirm & Start Ride");
            Toast.makeText(ConfirmRideActivity.this, "Connection Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String formatMoney(double amount) {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(amount);
    }
}