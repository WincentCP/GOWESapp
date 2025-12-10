package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    // Dynamic Views for Toggle
    private LinearLayout linkCardPrompt;
    private LinearLayout linkedCardDetails;
    private TextView tvPayAsYouGoTitle;

    // Data
    private String bikeId;
    private String slotKey;
    private double walletBalance = 0.0;
    private static final double HOURLY_RATE = 8000.0;
    private static final double DISCOUNT_RATE = 0.05; // 5%
    private static final int MIN_TOP_UP_AMOUNT = 10000;

    // State
    private String selectedMethod = "Wallet"; // Default
    private boolean isCardLinked = false;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_your_ride);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bikeId = getIntent().getStringExtra("BIKE_ID");
        slotKey = getIntent().getStringExtra("SLOT_KEY");

        initViews();
        setupListeners();
        updateBikeInfo();
        fetchUserData();
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

        // Find toggleable views
        linkCardPrompt = findViewById(R.id.link_card_prompt);
        linkedCardDetails = findViewById(R.id.linked_card_details);

        // Find Title dynamically based on XML structure provided
        if (payAsYouGoCard != null && payAsYouGoCard.getChildCount() > 0) {
            View child0 = payAsYouGoCard.getChildAt(0); // Main LinearLayout
            if (child0 instanceof LinearLayout) {
                LinearLayout mainContainer = (LinearLayout) child0;
                View childHeader = mainContainer.getChildAt(0); // Horizontal Header
                if (childHeader instanceof LinearLayout) {
                    LinearLayout header = (LinearLayout) childHeader;
                    // XML: Radio(0), Icon(1), Title(2), Chip(3)
                    if (header.getChildCount() > 2) {
                        View titleView = header.getChildAt(2);
                        if (titleView instanceof TextView) {
                            tvPayAsYouGoTitle = (TextView) titleView;
                        }
                    }
                }
            }
        }

        btnInlineTopUp = findViewById(R.id.btn_inline_top_up);
        btnConfirmRide = findViewById(R.id.btn_confirm);
        layoutDiscount = findViewById(R.id.layout_discount);
    }

    private void setupListeners() {
        if(ivBack != null) ivBack.setOnClickListener(v -> finish());

        if(payAsYouGoCard != null) payAsYouGoCard.setOnClickListener(v -> {
            if (isCardLinked) {
                selectPaymentMethod("PayAsYouGo");
            } else {
                showLinkCardDialog();
            }
        });

        if(useWalletCard != null) useWalletCard.setOnClickListener(v -> selectPaymentMethod("Wallet"));

        if(btnConfirmRide != null) btnConfirmRide.setOnClickListener(v -> startRide());

        if(btnInlineTopUp != null) {
            btnInlineTopUp.setOnClickListener(v -> showTopUpDialog());
        }
    }

    private void updateBikeInfo() {
        if (tvBikeName == null) return;
        if (bikeId != null) {
            String display = bikeId.startsWith("Bike") || bikeId.startsWith("BK") ? bikeId : "Bike " + bikeId;
            tvBikeName.setText(display);
        } else {
            tvBikeName.setText("Gowes Electric Bike");
        }
        if(tvStandardRate != null) tvStandardRate.setText("Rp " + formatMoney(HOURLY_RATE));
    }

    private void selectPaymentMethod(String method) {
        this.selectedMethod = method;

        if ("Wallet".equals(method)) {
            if(payAsYouGoCard != null) payAsYouGoCard.setBackgroundResource(R.drawable.card_payment_unselected);
            if(radioBtnPayg != null) {
                radioBtnPayg.setImageResource(isCardLinked ? R.drawable.ic_radio_button_unchecked : R.drawable.ic_add);
            }

            if(useWalletCard != null) useWalletCard.setBackgroundResource(R.drawable.card_payment_selected);
            if(radioBtnWallet != null) radioBtnWallet.setImageResource(R.drawable.ic_radio_button_checked);

            if(layoutDiscount != null) layoutDiscount.setVisibility(View.VISIBLE);
            if(tvDiscountAmount != null) tvDiscountAmount.setText("-Rp " + formatMoney(HOURLY_RATE * DISCOUNT_RATE));

        } else {
            if(payAsYouGoCard != null) payAsYouGoCard.setBackgroundResource(R.drawable.card_payment_selected);
            if(radioBtnPayg != null) radioBtnPayg.setImageResource(R.drawable.ic_radio_button_checked);

            if(useWalletCard != null) useWalletCard.setBackgroundResource(R.drawable.card_payment_unselected);
            if(radioBtnWallet != null) radioBtnWallet.setImageResource(R.drawable.ic_radio_button_unchecked);

            if(layoutDiscount != null) layoutDiscount.setVisibility(View.GONE);
        }
    }

    private void fetchUserData() {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null && snapshot.exists()) {
                        Double bal = snapshot.getDouble("walletBalance");
                        walletBalance = bal != null ? bal : 0.0;
                        if (tvWalletBalance != null) tvWalletBalance.setText("Balance: Rp " + formatMoney(walletBalance));

                        Boolean linked = snapshot.getBoolean("isCardLinked");
                        isCardLinked = Boolean.TRUE.equals(linked);

                        updateCardUI();
                    }
                });
    }

    private void updateCardUI() {
        if (!isCardLinked) {
            // State: Not Linked -> Show "Link Card" prompts
            if (tvPayAsYouGoTitle != null) {
                tvPayAsYouGoTitle.setText("Link Credit Card");
                tvPayAsYouGoTitle.setTextColor(getResources().getColor(R.color.primaryOrange));
            }
            if (radioBtnPayg != null) radioBtnPayg.setImageResource(R.drawable.ic_add);

            // Show prompt, hide details
            if (linkCardPrompt != null) linkCardPrompt.setVisibility(View.VISIBLE);
            if (linkedCardDetails != null) linkedCardDetails.setVisibility(View.GONE);

            // Force reset selection if user was on unlinked card
            if ("PayAsYouGo".equals(selectedMethod)) {
                selectPaymentMethod("Wallet");
            }
        } else {
            // State: Linked -> Show Card Details
            if (tvPayAsYouGoTitle != null) {
                tvPayAsYouGoTitle.setText("Pay-as-you-go");
                tvPayAsYouGoTitle.setTextColor(getResources().getColor(R.color.textPrimary));
            }
            if (radioBtnPayg != null) {
                radioBtnPayg.setImageResource("PayAsYouGo".equals(selectedMethod) ?
                        R.drawable.ic_radio_button_checked : R.drawable.ic_radio_button_unchecked);
            }

            // Hide prompt, show details
            if (linkCardPrompt != null) linkCardPrompt.setVisibility(View.GONE);
            if (linkedCardDetails != null) linkedCardDetails.setVisibility(View.VISIBLE);
        }
    }

    private void showLinkCardDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_link_payment, null);
        dialog.setContentView(view);

        // BUG FIX: Updated ID to 'btn_link' to match your XML
        View btnLink = view.findViewById(R.id.btn_link);
        View btnCancel = view.findViewById(R.id.btn_cancel);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if(btnLink != null) {
            btnLink.setOnClickListener(v -> {
                // Here we would normally validate inputs
                // For now, proceed to link
                db.collection("users").document(mAuth.getCurrentUser().getUid())
                        .update("isCardLinked", true)
                        .addOnSuccessListener(a -> {
                            Toast.makeText(this, "Card Linked Successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to link card", Toast.LENGTH_SHORT).show());
            });
        }
        dialog.show();
    }

    private void showTopUpDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_top_up_balance);

        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnTopUp = dialog.findViewById(R.id.btn_pay_now); // Assuming ID based on previous context
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
            if (walletBalance < 10000) {
                Toast.makeText(this, "Insufficient balance. Please top up.", Toast.LENGTH_LONG).show();
                return;
            }
        } else if ("PayAsYouGo".equals(selectedMethod) && !isCardLinked) {
            showLinkCardDialog();
            return;
        }

        if (slotKey == null || slotKey.isEmpty()) {
            Toast.makeText(this, "Error: Slot ID missing. Please rescan.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmRide.setEnabled(false);
        btnConfirmRide.setText("Unlocking Station...");

        DatabaseReference slotRef = FirebaseDatabase.getInstance("https://smartbike-c6082-default-rtdb.firebaseio.com/")
                .getReference("stations/station_uph_medan/slots")
                .child(slotKey);

        Map<String, Object> updates = new HashMap<>();
        updates.put("servoStatus", "OPEN");
        updates.put("status", "InUse");

        slotRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(ConfirmRideActivity.this, "Station Unlocked!", Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                slotRef.child("servoStatus").setValue("LOCKED");

                Intent intent = new Intent(ConfirmRideActivity.this, ActiveRideActivity.class);
                intent.putExtra("IS_NEW_RIDE", true);
                intent.putExtra("BIKE_ID", bikeId);
                intent.putExtra("BIKE_MODEL", tvBikeName != null ? tvBikeName.getText().toString() : bikeId);
                intent.putExtra("SLOT_KEY", slotKey);
                intent.putExtra(RideCompleteActivity.EXTRA_PAYMENT_METHOD, selectedMethod);

                startActivity(intent);
                finish();

            }, 3000);

        }).addOnFailureListener(e -> {
            btnConfirmRide.setEnabled(true);
            btnConfirmRide.setText("Confirm & Start Ride");
            Toast.makeText(ConfirmRideActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String formatMoney(double amount) {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(amount);
    }
}