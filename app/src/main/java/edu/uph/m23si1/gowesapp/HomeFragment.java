package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int MIN_TOP_UP_AMOUNT = 30000;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration userListener;

    private CardView activeRideBanner;
    private TextView tvUserName, tvWalletBalance;
    private TextView tvTotalRides, tvTimeSaved, tvCo2Saved;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        activeRideBanner = view.findViewById(R.id.active_ride_banner);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvWalletBalance = view.findViewById(R.id.tv_wallet_balance_main);

        tvTotalRides = view.findViewById(R.id.tv_total_rides);
        tvTimeSaved = view.findViewById(R.id.tv_time_saved);
        tvCo2Saved = view.findViewById(R.id.tv_co2_saved);

        activeRideBanner.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ActiveRideActivity.class);
            startActivity(intent);
        });

        View btnTopUp = view.findViewById(R.id.btn_top_up);
        if (btnTopUp != null) {
            btnTopUp.setOnClickListener(v -> showTopUpDialog());
        }

        return view;
    }

    private void showTopUpDialog() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        dialog.setContentView(R.layout.dialog_top_up_balance);

        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnTopUp = dialog.findViewById(R.id.btn_top_up);
        EditText etAmount = dialog.findViewById(R.id.et_amount);

        // Preset Buttons Logic
        View.OnClickListener presetListener = v -> {
            if (etAmount != null && v instanceof Button) {
                String text = ((Button) v).getText().toString(); // e.g. "Rp 30.000"
                String cleanAmount = text.replaceAll("[^0-9]", "");
                etAmount.setText(cleanAmount);
                etAmount.setSelection(etAmount.getText().length());
            }
        };

        int[] presetIds = {
                R.id.btn_amount_30k,
                R.id.btn_amount_50k,
                R.id.btn_amount_100k,
                R.id.btn_amount_200k
        };
        for (int id : presetIds) {
            View btn = dialog.findViewById(id);
            if (btn != null) btn.setOnClickListener(presetListener);
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnTopUp != null && etAmount != null) {
            btnTopUp.setOnClickListener(v -> {
                String amountStr = etAmount.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                int amount;
                try {
                    amount = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amount < MIN_TOP_UP_AMOUNT) {
                    Toast.makeText(getContext(), "Minimum Rp 30.000", Toast.LENGTH_SHORT).show();
                } else {
                    processTopUp(amount, dialog);
                }
            });
        }
        dialog.show();
    }

    private void processTopUp(int amountToAdd, BottomSheetDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DocumentReference userDocRef = db.collection("users").document(user.getUid());

        userDocRef.get().addOnSuccessListener(snapshot -> {
            int currentBalance = 0;
            if (snapshot.exists()) {
                Number currentBalanceNum = (Number) snapshot.get("walletBalance");
                if (currentBalanceNum != null) currentBalance = currentBalanceNum.intValue();
            }

            int newBalance = currentBalance + amountToAdd;

            Map<String, Object> data = new HashMap<>();
            data.put("walletBalance", newBalance);

            // Use merge so it creates or updates safely
            userDocRef.set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Top Up Successful!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Top up failed", e);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "onStart: current user uid = " + user.getUid());
            startUserListener(user.getUid());
        } else {
            goToSplash();
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
                String fullName = snapshot.getString("fullName");
                if (fullName != null && !fullName.isEmpty()) {
                    String[] parts = fullName.split(" ");
                    if (parts.length > 0) tvUserName.setText(parts[0]);
                    else tvUserName.setText(fullName);
                }

                Number balanceNum = (Number) snapshot.get("walletBalance");
                int balance = (balanceNum != null) ? balanceNum.intValue() : 0;
                if (tvWalletBalance != null) tvWalletBalance.setText(formatCurrency(balance));

                Boolean isActiveRide = snapshot.getBoolean("isActiveRide");
                if (Boolean.TRUE.equals(isActiveRide)) {
                    activeRideBanner.setVisibility(View.VISIBLE);
                } else {
                    activeRideBanner.setVisibility(View.GONE);
                }

                // Static stats for now
                tvTotalRides.setText("24");
                tvTimeSaved.setText("8.5h");
                tvCo2Saved.setText("12kg");
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void goToSplash() {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String formatCurrency(int amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0);
        return currencyFormatter.format(amount);
    }
}