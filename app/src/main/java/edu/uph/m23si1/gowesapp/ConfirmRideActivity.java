package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

public class ConfirmRideActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmRideActivity";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration userListener;
    private String userId;

    private CardView payAsYouGoCard, useWalletCard;
    private ImageView radioPayAsYouGo, radioUseWallet;
    private LinearLayout linkCardPrompt, linkedCardDetails;
    private TextView tvWalletBalance;
    private Button btnConfirm, btnLinkCard;

    private boolean isWalletSelected = false;
    private int walletBalance = 0;
    private boolean isCardLinked = false;

    private static final int INITIAL_HOLD_AMOUNT = 25000;
    private static final int MIN_TOP_UP_AMOUNT = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_your_ride);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Find all views
        payAsYouGoCard = findViewById(R.id.pay_as_you_go_card);
        useWalletCard = findViewById(R.id.use_wallet_card);
        radioPayAsYouGo = findViewById(R.id.radio_btn_payg);
        radioUseWallet = findViewById(R.id.radio_btn_wallet);
        linkCardPrompt = findViewById(R.id.link_card_prompt);
        linkedCardDetails = findViewById(R.id.linked_card_details);
        tvWalletBalance = findViewById(R.id.tv_wallet_balance);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnLinkCard = findViewById(R.id.btn_link_card);

        // Click listeners
        payAsYouGoCard.setOnClickListener(v -> {
            isWalletSelected = false;
            updatePaymentSelection();
        });

        useWalletCard.setOnClickListener(v -> {
            isWalletSelected = true;
            updatePaymentSelection();
        });

        btnLinkCard.setOnClickListener(v -> {
            // (Perbaikan Bug 5)
            Toast.makeText(this, "Membuka layar 'Tautkan Kartu'...", Toast.LENGTH_SHORT).show();
            // TODO: Buka Activity untuk menautkan kartu
            // Untuk demo, kita tautkan kartu di Firebase:
            if (userId != null) {
                db.collection("users").document(userId).update("isCardLinked", true);
            }
        });

        btnConfirm.setOnClickListener(v -> {
            if (isWalletSelected) {
                if (walletBalance < INITIAL_HOLD_AMOUNT) {
                    showInsufficientBalanceDialog();
                } else {
                    startRide();
                }
            } else {
                if (isCardLinked) {
                    startRide();
                } else {
                    Toast.makeText(this, "Silakan tautkan kartu terlebih dahulu", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            startUserListener(userId);
        } else {
            finish(); // Pengguna tidak login, tutup aktivitas ini
        }
    }

    private void startUserListener(String userId) {
        final DocumentReference userDocRef = db.collection("users").document(userId);

        userListener = userDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                // (Perbaikan Bug 8) Ambil data asli dari Firestore
                Number balanceNum = (Number) snapshot.get("walletBalance");
                Boolean cardLinked = snapshot.getBoolean("isCardLinked");

                walletBalance = (balanceNum != null) ? balanceNum.intValue() : 0;
                isCardLinked = (cardLinked != null) ? cardLinked : false;

                updateUI();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void updateUI() {
        tvWalletBalance.setText("Saldo saat ini: " + formatCurrency(walletBalance));

        if (isCardLinked) {
            linkedCardDetails.setVisibility(View.VISIBLE);
            linkCardPrompt.setVisibility(View.GONE);
        } else {
            linkedCardDetails.setVisibility(View.GONE);
            linkCardPrompt.setVisibility(View.VISIBLE);
        }

        // Default ke Pay As You Go jika kartu tertaut, jika tidak, default ke Dompet
        isWalletSelected = !isCardLinked;
        updatePaymentSelection();
    }

    // ... (Metode updatePaymentSelection() dan startRide() tidak berubah) ...

    private void updatePaymentSelection() {
        if (isWalletSelected) {
            // Select Wallet
            useWalletCard.setBackgroundResource(R.drawable.card_payment_selected);
            radioUseWallet.setImageResource(R.drawable.ic_radio_button_checked);
            // Unselect Pay-as-you-go
            payAsYouGoCard.setBackgroundResource(R.drawable.card_payment_unselected);
            radioPayAsYouGo.setImageResource(R.drawable.ic_radio_button_unchecked);

            // Enable confirm button only if balance is sufficient
            btnConfirm.setEnabled(walletBalance >= INITIAL_HOLD_AMOUNT);
        } else {
            // Select Pay-as-you-go
            payAsYouGoCard.setBackgroundResource(R.drawable.card_payment_selected);
            radioPayAsYouGo.setImageResource(R.drawable.ic_radio_button_checked);
            // Unselect Wallet
            useWalletCard.setBackgroundResource(R.drawable.card_payment_unselected);
            radioUseWallet.setImageResource(R.drawable.ic_radio_button_unchecked);

            // Enable confirm button only if card is linked
            btnConfirm.setEnabled(isCardLinked);
        }
    }

    private void startRide() {
        // (Perbaikan Bug 3) Set perjalanan aktif di Firestore
        if (userId != null) {
            db.collection("users").document(userId).update("isActiveRide", true);
        }

        Intent intent = new Intent(ConfirmRideActivity.this, ActiveRideActivity.class);
        startActivity(intent);
        finish();
    }


    // --- DIALOGS ---

    private void showInsufficientBalanceDialog() {
        // ... (Logika dialog tidak berubah) ...
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_insufficient_balance);

        Button topUpNow = dialog.findViewById(R.id.btn_top_up_now);
        Button maybeLater = dialog.findViewById(R.id.btn_maybe_later);

        topUpNow.setOnClickListener(v -> {
            dialog.dismiss();
            showTopUpDialog(); // Open the next dialog
        });

        maybeLater.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showTopUpDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_top_up_balance);

        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnTopUp = dialog.findViewById(R.id.btn_top_up);
        EditText etAmount = dialog.findViewById(R.id.et_amount);

        // (Perbaikan Bug 9)
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnTopUp.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Silakan masukkan jumlah", Toast.LENGTH_SHORT).show();
                return;
            }

            int amount = Integer.parseInt(amountStr);
            if (amount < MIN_TOP_UP_AMOUNT) {
                Toast.makeText(this, "Minimum top-up adalah " + formatCurrency(MIN_TOP_UP_AMOUNT), Toast.LENGTH_SHORT).show();
            } else {
                // (Perbaikan Bug 9) Update saldo di Firebase
                if (userId != null) {
                    DocumentReference userDocRef = db.collection("users").document(userId);
                    db.runTransaction(transaction -> {
                        DocumentSnapshot snapshot = transaction.get(userDocRef);
                        Number currentBalanceNum = (Number) snapshot.get("walletBalance");
                        int currentBalance = (currentBalanceNum != null) ? currentBalanceNum.intValue() : 0;
                        int newBalance = currentBalance + amount;
                        transaction.update(userDocRef, "walletBalance", newBalance);
                        return newBalance; // Mengembalikan saldo baru
                    }).addOnSuccessListener(newBalance -> {
                        Toast.makeText(this, "Top Up Berhasil! Saldo baru: " + formatCurrency((Integer) newBalance), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Top Up Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });

        etAmount.setText(String.valueOf(MIN_TOP_UP_AMOUNT));
        dialog.show();
    }

    private String formatCurrency(int amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0);
        return currencyFormatter.format(amount);
    }
}